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

import com.alibaba.sparkcube.optimizer.CacheIdentifier

// UIUtils is private to spark
case class SparkCubeInformation(
    viewDataBase: String,
    viewName: String,
    cacheName: String,
    enableRewrite: Boolean,
    cacheType: String,
    cacheDataSize: String,
    lastUpdateTime: Long,
    detailUrl: String,
    fullCacheName: String)

class SparkCubePage(parent: SparkCubeTab)
  extends AbstractSparkCubePage(parent, "") {

  override def render(request: HttpServletRequest): Seq[Node] = {

    val cacheManager = parent.sharedState.cubeManager
    val action = UIUtils.stripXSS(request.getParameter("action"))
    if (action != null && action.nonEmpty) {
      if (action == "drop") {
        val parameterCacheId = UIUtils.stripXSS(request.getParameter("cacheId"))
        require(parameterCacheId != null && parameterCacheId.nonEmpty, "Missing cache id parameter")
        val cacheId = CacheIdentifier(parameterCacheId)
        cacheManager.dropCache(sparkSession, cacheId)
      }
    }

    val caches = cacheManager.listAllCaches(sparkSession)
    var cachedDataInfo = Seq[SparkCubeInformation]()
    for(cache <- caches) {
      val viewDataBase = cache._1.database.getOrElse("")
      val viewName = cache._1.table
      var cacheName: String = null
      var enableRewrite: Boolean = false
      var cacheType: String = null
      var cacheDataSize: String = ""
      var lastUpdateTime: Long = -1
      var detailUrl = ""
      if (cache._2.rawCacheInfo.isDefined) {
        cacheName = cache._2.rawCacheInfo.get.cacheName
        detailUrl = jobURL(request, s"$viewDataBase.$viewName.$cacheName")
        enableRewrite = cache._2.rawCacheInfo.get.enableRewrite
        cacheType = "Raw Cache"
        cacheDataSize = getFileSize(cache._2.rawCacheInfo.get.storageInfo.storagePath)
        lastUpdateTime = cache._2.rawCacheInfo.get.lastUpdateTime
        val tempRelation = SparkCubeInformation(
          viewDataBase, viewName, cacheName, enableRewrite,
          cacheType, cacheDataSize, lastUpdateTime, detailUrl,
          s"$viewDataBase.$viewName.$cacheName")
        cachedDataInfo = cachedDataInfo :+ tempRelation
      }

      if (cache._2.cubeCacheInfo.isDefined) {
        cacheName = cache._2.cubeCacheInfo.get.cacheName
        detailUrl = jobURL(request, s"$viewDataBase.$viewName.$cacheName")
        enableRewrite = cache._2.cubeCacheInfo.get.enableRewrite
        cacheType = "Cube Cache"
        cacheDataSize = getFileSize(cache._2.cubeCacheInfo.get.storageInfo.storagePath)
        lastUpdateTime = cache._2.cubeCacheInfo.get.lastUpdateTime
        val tempRelation = SparkCubeInformation(
          viewDataBase, viewName, cacheName, enableRewrite,
          cacheType, cacheDataSize, lastUpdateTime, detailUrl,
          s"$viewDataBase.$viewName.$cacheName")
        cachedDataInfo = cachedDataInfo :+ tempRelation
      }
    }
    val content = generateLoadResources(request) ++ cacheTable(cachedDataInfo) ++
      addCreateButtonHtml(request) :+
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
              <a href={cubeUrl(request)} class="btn btn-primary">Close</a>
            </div>
          </div>
        </div>
      </div>
    UIUtils.headerSparkPage(request, "Cube Management", content, parent)
  }

  private def addCreateButtonHtml(request: HttpServletRequest): Seq[Node] = {
    <div>
      <a href={s"${UIUtils.prependBaseUri(request, parent.basePath)}/${parent.prefix}/Create"}
         class="btn btn-default">
        New Cache
      </a>
    </div>
  }

  private def cacheTable(rdds: Seq[SparkCubeInformation]): Seq[Node] = {
    if (rdds.isEmpty) {
      // Don't show the rdd table if there is no RDD persisted.
      Nil
    } else {
      <div>
        {UIUtils.listingTable(rcHeader, cacheRow, rdds, id = Some("cache-table"))}
      </div>
    }
  }

  /** Render an HTML row representing an Cube Management */
  private def cacheRow(cache: SparkCubeInformation): Seq[Node] = {
    val onclickFunc = "dropCache('" + {cache.fullCacheName} + "')"
    // scalastyle:off
    <tr>
      <td>{cache.viewDataBase}</td>
      <td>{cache.viewName}</td>
      <td>{cache.cacheName}</td>
      <td>{cache.enableRewrite}</td>
      <td>{cache.cacheType}</td>
      <td>{cache.cacheDataSize}</td>
      <td>{UIUtils.formatDate(cache.lastUpdateTime)}</td>
      <td><a href={cache.detailUrl}>{"Detail"}</a>
        <span>&nbsp;</span>
        <button class="btn btn-link" type="button" onclick={onclickFunc}>Drop</button>
      </td>
    </tr>
    // scalastyle:on
  }

  /** Header fields for the Rational Cache */
  private val rcHeader = Seq(
    "viewDataBase",
    "viewName",
    "cacheName",
    "enableRewrite",
    "cacheType",
    "cacheDataSize",
    "lastUpdataTime",
    "action"
  )

  private def jobURL(request: HttpServletRequest, cacheId: String): String =
    "%s/Cube Management/Detail/?cacheId=%s"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheId)

  private def dropUrl(request: HttpServletRequest, cacheId: String): String = {
    "%s/Cube Management/?action=drop&cacheId=%s"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheId)
  }

  private def cubeUrl(request: HttpServletRequest): String = {
    "%s/Cube Management"
      .format(UIUtils.prependBaseUri(request, parent.basePath))
  }

  private def generateLoadResources(request: HttpServletRequest): Seq[Node] = {
    // scalastyle:off
      <script src={UIUtils.prependBaseUri(request, "/static/bootstrap.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request,
        "/static/SparkCube/caching/spark-cube-page.js")}></script>
    // scalastyle:on
  }
}
