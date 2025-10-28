package com.ronaldbit.spring;

import com.ronaldbit.mopla.Mopla;
import com.ronaldbit.mopla.MoplaContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.View;

import java.util.Map;

public class MoplaView implements View {
  private final Mopla mopla;
  private final String template;

  public MoplaView(Mopla mopla, String template) {
    this.mopla = mopla;
    this.template = template;
  }

  @Override
  public String getContentType() { return "text/html; charset=UTF-8"; }

  @Override
  public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
    MoplaContext ctx = new MoplaContext();
    // copiar session attributes
    var session = request.getSession(false);
    if (session != null) {
      var names = session.getAttributeNames();
      while (names.hasMoreElements()) {
        String n = names.nextElement(); ctx.session().put(n, session.getAttribute(n));
      }
    }
    // request params
    request.getParameterMap().forEach((k,v) -> { if (v!=null && v.length>0) ctx.req().put(k, v.length==1? v[0] : java.util.Arrays.asList(v)); });

    String out = mopla.render(template, ctx, (Map<String,Object>) model);
    response.setContentType(getContentType());
    response.getWriter().write(out);
  }
}
