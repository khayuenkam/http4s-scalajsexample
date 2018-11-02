package org.http4s.scalajsexample

import cats.implicits._
import cats.data._
import cats.effect._
import io.circe.syntax._
import org.http4s._
import org.http4s.CacheDirective._
import org.http4s.MediaType
import org.http4s.dsl.Http4sDsl
import org.http4s.headers._
import org.http4s.circe._
import scala.concurrent.ExecutionContext.global
import scalatags.Text.TypedTag
import scalatags.Text.all.Modifier

object JSApplication {

  val jsScript = "http4s-scalajsexample-frontend-fastopt.js"
  val jsDeps = "http4s-scalajsexample-frontend-jsdeps.js"
  val jsScripts: Seq[Modifier] = {
    import scalatags.Text.all._
    List(
      script(src := jsScript),
      script(src := jsDeps)
    )
  }

  val index: Seq[Modifier] = {
    import scalatags.Text.all._
    Seq(
      h1(
        style:= "align: center;",
        "Http4s Scala-js Example App"
      ),
      a(href:="/button", h4("Button Example")),
      a(href:="/ajax", h4("Ajax Example"))
    )
  }

  val buttonTag: Seq[Modifier] = {
    import scalatags.Text.all._
    Seq(
      h1("Push The Button"),
      a(href:="/", h4("Home")),
      button(
        id := "click-me-button",
        `type` := "button",
        onclick := "addClickedMessage()",
        style := "background-color: #4CAF50; /* Green */ " +
          "border: none; " +
          "border-radius: 12px; " +
          "color: white; " +
          "padding: 15px 32px; " +
          "text-align: center; " +
          "text-decoration: none; " +
          "display: inline-block; " +
          "font-size: 16px;",
        "Click Me"
      )
    )
  }

  val ajaxTag: Seq[Modifier] = {
      import scalatags.Text.all._
      Seq(
        h1("Push The Button"),
        a(href:="/", h4("Home")),
        button(
          id := "click-me-button",
          `type` := "button",
          onclick := "addAjaxCall()",
          style := "background-color: #4CAF50; /* Green */ " +
            "border: none; " +
            "border-radius: 12px; " +
            "color: white; " +
            "padding: 15px 32px; " +
            "text-align: center; " +
            "text-decoration: none; " +
            "display: inline-block; " +
            "font-size: 16px;",
          "Click Me"
        )
      )
    }

  def template(
      headContent: Seq[Modifier],
      bodyContent: Seq[Modifier],
      scripts: Seq[Modifier],
      cssComps: Seq[Modifier]): TypedTag[String] = {
    import scalatags.Text.all._

    html(
      head(
        headContent,
        cssComps,
        link(rel:="shortcut icon", media:="image/png", href:="/assets/images/favicon.png")
      ),
      body(
        bodyContent,
        scripts
      )
    )

  }

  val supportedStaticExtensions =
    List(".html", ".js", ".map", ".css", ".png", ".ico")

  def service[F[_]](implicit F: Effect[F], cs: ContextShift[F]) = {
    def getResource(pathInfo: String) = F.delay(getClass.getResource(pathInfo))

    object dsl extends Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {

      case GET -> Root =>
        Ok(template(Seq(), index, jsScripts, Seq()).render)
          .map(
            _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`))
              .putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`())))
          )

      case GET -> Root / "button" =>
        Ok(template(Seq(), buttonTag, jsScripts, Seq()).render)
          .map(
            _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`))
              .putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`())))
          )

      case GET -> Root / "ajax" =>
        Ok(template(Seq(), ajaxTag, jsScripts, Seq()).render)
          .map(
            _.withContentType(`Content-Type`(MediaType.text.html, Charset.`UTF-8`))
              .putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`())))
          )

      case GET -> Root / "json" / name =>
        Ok(MyData(name).asJson)

      case req if supportedStaticExtensions.exists(req.pathInfo.endsWith) =>
        StaticFile.fromResource[F](req.pathInfo, global, req.some)
          .orElse(OptionT.liftF(getResource(req.pathInfo)).flatMap(StaticFile.fromURL[F](_, global, req.some)))
          .map(_.putHeaders(`Cache-Control`(NonEmptyList.of(`no-cache`()))))
          .fold(NotFound())(_.pure[F])
          .flatten

    }.orNotFound
  }

}
