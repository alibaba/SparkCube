/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.ui

import javax.servlet.http.HttpServletRequest

import scala.xml.Node

import com.alibaba.sparkcube.catalog.{CubeCacheInfo, RawCacheInfo}
import com.alibaba.sparkcube.execution._
import com.alibaba.sparkcube.optimizer.CacheIdentifier


// UIUtils is private to spark
class SparkCubeDetailPage(parent: SparkCubeTab)
  extends AbstractSparkCubePage(parent, "Detail") {

  override def render(request: HttpServletRequest): Seq[Node] = {
    val parameterCacheId = UIUtils.stripXSS(request.getParameter("cacheId"))
    require(parameterCacheId != null && parameterCacheId.nonEmpty,
      "Missing cache id parameter")

    val cacheId = CacheIdentifier(parameterCacheId)
    val cacheManager = parent.sharedState.cubeManager

    val optionalAutoBuildCacheInfo = cacheManager.getAutoBuildCache(cacheId)
    val periodBuildInfoData = optionalAutoBuildCacheInfo match {
      case Some(strDateTime: StringDatePeriodBuildInfo) =>
        Seq(
          "Save Mode" -> strDateTime.saveMode.toString,
          "Trigger Time" -> UIUtils.formatDate(strDateTime.triggerTime),
          "Period" -> strDateTime.period.toString,
          "Period Unit" -> strDateTime.periodUnit.toString,
          "Step Column" -> strDateTime.columnName,
          "Step Column DateFormat" -> strDateTime.columnDateFormat
        )

      case Some(fullInfo: FullPeriodUpdateInfo) =>
        Seq(
          "Save Mode" -> fullInfo.saveMode.toString,
          "Trigger Time" -> UIUtils.formatDate(fullInfo.triggerTime),
          "Period" -> fullInfo.period.toString,
          "Period Unit" -> fullInfo.periodTimeUnit.toString
        )
      case Some(timestamp: TimeStampPeriodUpdateInfo) =>
        Seq(
          "Save Mode" -> timestamp.saveMode.toString,
          "Trigger Time" -> UIUtils.formatDate(timestamp.triggerTime),
          "Period" -> timestamp.period.toString,
          "Period Unit" -> timestamp.periodTimeUnit.toString,
          "Step Column" -> timestamp.columnName
        )
      case _ =>
        Seq.empty
    }

    val periodBuildNode = if (periodBuildInfoData.isEmpty) {
      <a href={buildCacheUrl(request, cacheId.toString)} class="btn btn-link">Build Cache</a>
      <a href={periodBuildCacheUrl(request, cacheId.toString)} class="btn btn-link">
        Trigger Period Build
      </a>
    } else {
      <a href={buildCacheUrl(request, cacheId.toString)} class="btn btn-link">Build Cache</a>
      <h5>Period Build Info:</h5>
      <div>
        {UIUtils.listingTable(propertyHeader, basicInfoRow, periodBuildInfoData,
          fixedWidth = false)}
      </div>
      <button class="btn btn-link" type="button" onclick="cancelPeriodBuild()">
        Cancel Period Build
      </button>
    }

    val resources = generateLoadResources(request)

    val cacheInfo = cacheManager.getCacheInfo(sparkSession, cacheId).get
    val isBuilding = if (cacheManager.isCacheUnderBuilding(cacheId)) {
      <p>Background job is <b>building cache now</b>.</p>
    } else {
      // scalastyle:off
      <p>Latest cache building is finished at {CacheUtils.formatTime(cacheInfo.getLastUpdateTime)}</p>
      // scalastyle:on
    }
    val basicInfo = cacheInfo match {
      case rawCacheInfo: RawCacheInfo =>
        Seq(
          "Database" -> cacheId.db,
          "View Name" -> cacheId.viewName,
          "Cache Name" -> cacheId.cacheName,
          "Enabled" -> rawCacheInfo.enableRewrite.toString,
          "Cache Type" -> "RAW",
          "Cache Columns" -> rawCacheInfo.cacheSchema.cols.mkString(", "),
          "Location" -> rawCacheInfo.storageInfo.storagePath,
          "Data Size" -> getFileSize(rawCacheInfo.storageInfo.storagePath),
          "Provider" -> rawCacheInfo.storageInfo.provider,
          "Partition By" -> rawCacheInfo.storageInfo.partitionSpec.map(_.mkString(", "))
            .getOrElse(""),
          "ZOrder By" -> rawCacheInfo.storageInfo.zorder.map(_.mkString(", ")).getOrElse(""),
          "Last Update Time" -> UIUtils.formatDate(rawCacheInfo.lastUpdateTime)
        )
      case cubeCacheInfo: CubeCacheInfo =>
        Seq(
          "Database" -> cacheId.db,
          "View Name" -> cacheId.viewName,
          "Cache Name" -> cacheId.cacheName,
          "Enabled" -> cubeCacheInfo.enableRewrite.toString,
          "Cache Type" -> "Cube",
          "Cube Dimensions" -> cubeCacheInfo.cacheSchema.dims.mkString(", "),
          "Cube Measures" -> cubeCacheInfo.cacheSchema.measures.mkString(", "),
          "Location" -> cubeCacheInfo.storageInfo.storagePath,
          "Data Size" -> getFileSize(cubeCacheInfo.storageInfo.storagePath),
          "Provider" -> cubeCacheInfo.storageInfo.provider,
          "Partition By" -> cubeCacheInfo.storageInfo.partitionSpec.map(_.mkString(", "))
            .getOrElse(""),
          "ZOrder By" -> cubeCacheInfo.storageInfo.zorder.map(_.mkString(", ")).getOrElse(""),
          "Last Update Time" -> UIUtils.formatDate(cubeCacheInfo.lastUpdateTime)
        )
    }

    val buildHistory = cacheManager.listBuildHistory(sparkSession, cacheId)

    val basicInfoTable = UIUtils.listingTable(
      propertyHeader, basicInfoRow, basicInfo, fixedWidth = true)



    val buildHistoryTable = UIUtils.listingTable(
      buildHistoryHeader, buildHistoryRow, buildHistory, fixedWidth = true,
      id = Some("cacheBuildHistoryTable"), sortable = true)

    val rewriteToggle = if (cacheInfo.getEnableRewrite) {
      <input id="rewriteToggle" type="checkbox" checked="true"
             data-toggle="toggle" data-on="Enabled" data-off="Disabled"
             data-onstyle="default" data-width="100" data-height="25"></input>
    } else {
      <input id="rewriteToggle" type="checkbox" data-toggle="toggle"
             data-on="Enabled" data-off="Disabled" data-onstyle="default"
             data-width="100" data-height="25"></input>
    }

    val partitionDiv = if (cacheInfo.getStorageInfo.partitionSpec.isDefined) {
      val partitions = cacheManager.listCachePartitions(sparkSession, cacheId)

      val partitionInfo = partitions.map {
        path =>
          val onclickFunc = "deleteCachePartition('" + {path} + "')"
          PartitionInfo(path, getFileSize(cacheInfo.getStorageInfo.storagePath + "/" + path),
            <button class="btn btn-link" type="button" onclick={onclickFunc}>Delete</button>)
      }
      val partitionTable = UIUtils.listingTable(
        partitionHeader, partitionInfoRow, partitionInfo, fixedWidth = true,
        id = Some("cachePartitionsListTable"), sortable = true)
      <div>
        <span class="expand-additional-metrics">
          <span class="expand-additional-metrics-arrow arrow-closed"></span>
          <a>Partition Information</a>
        </span>
        <div class="additional-metrics">
          {partitionTable}
        </div>
      </div>
    } else {
      <div></div>
    }

    val content = resources ++
      <span>
        <div>
          <span class="expand-additional-metrics">
            <span class="expand-additional-metrics-arrow arrow-closed"></span>
            <a>Basic Cache Information</a>
          </span>
          <div class="additional-metrics">
            <p id="cacheId" hidden="true" value={cacheId.toString}>{cacheId.toString}</p>
            {basicInfoTable}{rewriteToggle}
          </div>
        </div>{partitionDiv}<div>
        <span class="expand-additional-metrics">
          <span class="expand-additional-metrics-arrow arrow-closed"></span>
          <a>Build Information</a>
        </span>
        <div class="additional-metrics">
          {isBuilding}
          {periodBuildNode}
          <h5>Build History</h5>
          {buildHistoryTable}
          <button class="btn btn-link" type="button" onclick="clearBuildHistory()">
            Clear Build History
          </button>
        </div>
      </div>
    </span>
    <div id="actionModal" class="modal hide" tabindex="-1" role="dialog"
         aria-labelledby="actionTitle" aria-hidden="true">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <div class="modal-header">
            <h5 class="modal-title" id="actionTitle">

            </h5>
          </div>
          <div class="modal-body">
            <p id="actionBody">

            </p>
          </div>
          <div class="modal-footer">
            <a href={detailUrl(request, cacheId.toString)} class="btn btn-primary">Close</a>
          </div>
        </div>
      </div>
    </div>
    UIUtils.headerSparkPage(request, "Cube Management Detail Page", content, parent,
      useDataTables = true)
  }

  private def propertyHeader = Seq("Name", "Value")
  private def partitionHeader = Seq("Path", "Size", "Action")
  private def buildHistoryHeader = Seq("StartTime", "EndTime", "Save Mode", "Build Condition",
    "Status", "Info")

  private def basicInfoRow(kv: (String, String)) = <tr><td>{kv._1}</td><td>{kv._2}</td></tr>
  private def partitionInfoRow(partInfo: PartitionInfo) =
    <tr>
      <td>{partInfo.path}</td>
      <td>{partInfo.size}</td>
      <td>{partInfo.deleteLink}</td>
    </tr>

  private def buildHistoryRow(buildHistory: BuildHistory) =
    <tr>
      <td>{UIUtils.formatDate(buildHistory.startTime)}</td>
      <td>{UIUtils.formatDate(buildHistory.endTime)}</td>
      <td>{buildHistory.saveMode.toString}</td>
      <td>{buildHistory.buildInfo.map(_.toString).getOrElse("")}</td>
      <td>{buildHistory.status}</td>
      <td>{buildHistory.errorMsg}</td>
    </tr>

  private def detailUrl(
      request: HttpServletRequest,
      cacheId: String): String =
    "%s/Cube Management/Detail/?cacheId=%s"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheId)

  private def buildCacheUrl(request: HttpServletRequest, cacheId: String): String = {
    "%s/Cube Management/Build/?cacheId=%s&action=build"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheId)
  }

  private def periodBuildCacheUrl(request: HttpServletRequest, cacheId: String): String = {
    "%s/Cube Management/Build/?cacheId=%s&action=trigger"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheId)
  }

  private def generateLoadResources(request: HttpServletRequest): Seq[Node] = {
    // scalastyle:off
      <link rel="stylesheet" href={UIUtils.prependBaseUri(request,
      "/static/contrib/bootstrap-toggle.min.css")} type="text/css"/>
      <link rel="stylesheet" href={UIUtils.prependBaseUri(request,
        "/static/caching/spark-cube-page.css")} type="text/css"/>
      <script src={UIUtils.prependBaseUri(request, "/static/contrib/bootstrap.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request,
        "/static/contrib/bootstrap-toggle.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request,
        "/static/caching/spark-cube-detail-page.js")}></script>
    // scalastyle:on
  }
}

case class PartitionInfo(path: String, size: String, deleteLink: Node)
