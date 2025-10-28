package com.ronaldbit.mopla;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Scopes persistentes: app (global), session (usuario), req (petición actual). */
public class MoplaContext {
  private final Map<String,Object> app = new ConcurrentHashMap<>();
  private final Map<String,Object> session = new ConcurrentHashMap<>();
  private final Map<String,Object> req = new ConcurrentHashMap<>();

  public Map<String,Object> app()     { return app; }
  public Map<String,Object> session() { return session; }
  public Map<String,Object> req()     { return req; }

  /** Limpia solo variables de request (útil por ciclo de render). */
  public void clearRequest() { req.clear(); }
}
