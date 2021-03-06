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

import com.google.gson.Gson;
import org.crsh.shell.Shell;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/** @author <a href="mailto:julien.viet@exoplatform.com">Julien Viet</a> */
@WebServlet(urlPatterns = "/execute", asyncSupported = true)
public class ExecuteServlet extends HttpServlet {

  /** . */
  private static final Logger log = Logger.getLogger(ExecuteServlet.class.getSimpleName());

  /** . */
  static final Map<String, Connection> connections = new ConcurrentHashMap<String, Connection>();

  @Override
  public void init() throws ServletException {
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    String data = req.getReader().readLine();
    if (data != null) {
      data = data.substring("data=".length());
      Event event = new Gson().fromJson(data, Event.class);
      Connection conn = connections.get(event.socket);
      if (conn != null) {
        conn.process(event);
      } else {
        // System.out.println("CONNECTION NOT FOUND " + event.socket);
      }
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    LifeCycle lf = LifeCycle.getLifeCycle(getServletContext());
    Session session = lf.getSession();
    Shell shell = session.getShell();

    //
    String id = req.getParameter("id");
    if (id != null) {
      String transport = req.getParameter("transport");
      AsyncContext context = req.startAsync();
      context.setTimeout(300 * 1000L); // 5 minutes

      //
      resp.setCharacterEncoding("utf-8");
      resp.setHeader("Access-Control-Allow-Origin", "*");
      resp.setContentType("text/" + ("sse".equals(transport) ? "event-stream" : "plain"));

      //
      PrintWriter writer = resp.getWriter();
      for (int i = 0; i < 2000; i++) {
        writer.print(' ');
      }
      writer.print("\n");
      writer.flush();

      //
      Connection conn = new Connection(this, context, shell, id, req.getRemoteHost());
      connections.put(id, conn);
      context.addListener(conn);
    } else {
      log.info(req.getRemoteHost() + " no connection id for request " + req.getRequestURL() + " " + req.getParameterMap());
    }
  }

  static class Event
  {

    /** . */
    String type;

    /** . */
    String socket;

    /** . */
    Object data;

    public Event() {
    }

    public Event(String type) {
      this.type = type;
    }

    public Event data(Object data) {
      this.data = data;
      return this;
    }

    public Event socket(String socket) {
      this.socket = socket;
      return this;
    }

    @Override
    public String toString()
    {
      return "Event[type=" + type + ", socket=" + socket + ", data=" + data + "]";
    }
  }
}
