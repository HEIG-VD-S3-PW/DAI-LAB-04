package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;

public interface ResourceControllerInterface {
  void all(Context ctx);

  void show(Context ctx);

  void create(Context ctx);

  void update(Context ctx);

  void delete(Context ctx);
}
