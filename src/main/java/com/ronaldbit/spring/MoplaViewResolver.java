package com.ronaldbit.spring;

import com.ronaldbit.mopla.Mopla;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;

import java.util.Locale;

public class MoplaViewResolver implements ViewResolver {
  private final Mopla mopla;
  private String suffix = ".html";

  public MoplaViewResolver(Mopla mopla) { this.mopla = mopla; }

  public void setSuffix(String s) { this.suffix = s; }

  @Override
  public View resolveViewName(String viewName, Locale locale) throws Exception {
    String tpl = viewName.endsWith(suffix) ? viewName : viewName + suffix;
    return new MoplaView(mopla, tpl);
  }
}
