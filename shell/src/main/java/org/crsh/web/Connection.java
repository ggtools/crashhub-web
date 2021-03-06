/*
 * Copyright (C) 2013 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */

package org.crsh.web;

import org.crsh.shell.Shell;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import java.io.IOException;
import java.util.Map;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
class Connection implements AsyncListener
{

  /** . */
  final ExecuteServlet servlet;

  /** . */
  final AsyncContext context;

  /** . */
  final String id;

  /** . */
  final Shell shell;

  /** . */
  ProcessContext current;

  /** . */
  final String remoteHost;

  Connection(ExecuteServlet servlet, AsyncContext context, Shell shell, String id, String remoteHost) {
    this.servlet = servlet;
    this.context = context;
    this.id = id;
    this.shell = shell;
    this.remoteHost = remoteHost;
  }

  void process(ExecuteServlet.Event event) {

    if("message".equals(event.type)) {

      //
      if (current != null) {
        // System.out.println("Duplicate process execution");
      } else {
        // Create a shell session if needed
        if (shell == null) {
          // Should use request principal :-)
        }

        //
        Map<String, Object> map = (Map<String, Object>) event.data;
        String line = (String)map.get("line");
        Double widthP = (Double)map.get("width");
        Double heightP = (Double)map.get("height");
        int width = 110;
        if (widthP != null) {
          width = widthP.intValue();
        }
        int height = 30;
        if (heightP != null) {
          height = heightP.intValue();
        }

        // Execute process and we are done
        current = new ProcessContext(remoteHost, this, line, width, height);
        current.begin();
      }
    } else {
      // System.out.println("Unhandled event " + event);
    }
  }

  public void onStartAsync(AsyncEvent event) throws IOException {
    // Should not be called
  }

  public void onComplete(AsyncEvent event) throws IOException {
    // System.out.println("onComplete " + id);
    servlet.connections.remove(id);
  }

  public void onTimeout(AsyncEvent event) throws IOException {
    // System.out.println("onTimeOut " + id);
    servlet.connections.remove(id);
  }

  public void onError(AsyncEvent event) throws IOException {
    // System.out.println("onError " + id);
    servlet.connections.remove(id);
  }
}
