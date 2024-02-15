/*
 *
 */

package org.apache.spark.flamegraph.ui

import fr.an.spark.plugin.flamegraph.driver.{FlameGraphDriverPlugin, FlameGraphService}
import org.apache.spark.internal.Logging
import org.apache.spark.ui.{SparkUI, SparkUITab, UIUtils, WebUIPage}

import javax.servlet.http.{HttpServlet, HttpServletRequest, HttpServletResponse}
import scala.xml.Node

/**
 *
 */
class FlameGraphUI(val flameGraphService: FlameGraphService, sparkUI: SparkUI)
  extends SparkUITab(sparkUI, "FlameGraph") with Logging {


  init()
  def init(): Unit = {
//    val flameGraphHttpServlet = new FlameGraphHttpServlet
//    sparkUI.attachHandler("/flameGraph", flameGraphHttpServlet, "")
    sparkUI.attachTab(new FlameGraphDriverTab(flameGraphService, sparkUI))


//    val contextHandler = new ServletContextHandler
//    contextHandler.setContextPath("/flamegraph-plugin/api")
//    contextHandler.addServlet(new ServletHolder(loaderServlet), "/*")
//    sparkUI.attachHandler(contextHandler)

  }

}


class FlameGraphHttpServlet extends HttpServlet with Logging {

  override def doGet(req: HttpServletRequest, resp: HttpServletResponse): Unit = {
    // TODO
    logInfo("FlameGraph http doGet");
  }

}

class FlameGraphDriverTab(flameGraphService: FlameGraphService, sparkUI: SparkUI)
  extends SparkUITab(sparkUI, "FlameGraph") {

    attachPage(new FlameGraphWebUIPage(flameGraphService, this));

}

class FlameGraphWebUIPage(flameGraphService: FlameGraphService,
                          parent: FlameGraphDriverTab)
  extends WebUIPage("") {

  override def render(request: HttpServletRequest): Seq[Node] = {
//    scala.xml.Node scalaNode;
//    try(val stream = FlameGraphWebUIPage.class.getResourceAsStream("FlameGraphWebUIPage.html")) {
//      scalaNode = (scala.xml.Node) scala.xml.XML.load(stream);
//    } catch (IOException ex) {
//      log.error("should not occur", ex);
//      scalaNode = (scala.xml.Node) scala.xml.XML.loadString("FAILED to load page");
//    }
//    val ls = new ArrayList<>(Arrays.asList(scalaNode));
//    return JavaConverters.asScalaBuffer(ls).toSeq();
    val content = <div>
      <link rel="stylesheet" type="text/css" href={UIUtils.prependBaseUri(request, "/flamegraph-plugin/static/d3-flamegraph.css")}></link>
      <script src={UIUtils.prependBaseUri(request, "/static/d3.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request, "/flamegraph-plugin/static/d3-flamegraph.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request, "/flamegraph-plugin/static/FlameGraph.js")}></script>

      <button onclick="onClickRefreshFlameGraph()">Refresh</button>
      <button onclick="onClickDummyFlameGraph()">Dummy</button>

      <div>
        <div>
          <span id="flamegraph-header1" style="cursor: pointer;">
            <h4>
              <span id="flamegraph-arrow" class="arrow-open"></span>
              <a>Flame Graph</a>
            </h4>
          </span>
        </div>
        <div id="flamegraph-chart1">
        </div>
      </div>
    </div>;
    // UIUtils.basicSparkPage(request, content, "FlameGraph", true)
    UIUtils.headerSparkPage(request, "FlameGraph", content, parent)
  }

}

