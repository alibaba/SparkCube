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

import java.io.FileNotFoundException
import javax.servlet.http.HttpServletRequest

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.fs.Path

import org.apache.spark.internal.Logging
import org.apache.spark.sql.{CubeSharedState, SparkSession}
import org.apache.spark.sql.execution.ui.SQLAppStatusStore

// SparkUITab is private to package spark
class SparkCubeTab(
    sqlStore: SQLAppStatusStore,
    sparkUI: SparkUI,
    val sharedState: CubeSharedState)
  extends SparkUITab(sparkUI, "Cube Management") with Logging {

  val parent = sparkUI
  attachPage(new SparkCubePage(this))
  attachPage(new SparkCubeDetailPage(this))
  attachPage(new SparkCubeBuildPage(this))
  attachPage(new SparkCubeCreatePage(this))
  parent.attachTab(this)
  parent.addStaticHandler(SparkCubeTab.STATIC_RESOURCE_DIR, "/static/SparkCube")
  parent.addStaticHandler(SparkCubeTab.STATIC_IMG_DIR, "/img")
}


object SparkCubeTab {
  private val STATIC_RESOURCE_DIR = "com/alibaba/sparkcube/execution/ui/static"
  private val STATIC_IMG_DIR = "com/alibaba/sparkcube/execution/ui/img"
}

abstract class AbstractSparkCubePage(parent: SparkCubeTab, prefix: String)
  extends WebUIPage(prefix) with Logging {
  val sparkSession = SparkSession.builder().getOrCreate()
  val conf = new Configuration()
  val useCacheDbs = sparkSession.sparkContext.conf
    .get("spark.sql.cache.useDatabase", "default").split(",").toSeq

  def getFileSize(path: String): String = {
    val cachePath = new Path(path)
    val fs = cachePath.getFileSystem(conf)
    var fileSize = 0L
    if (fs.exists(cachePath)) {
      try {
        fileSize = fs.getContentSummary(cachePath).getLength()
      } catch {
        case e : FileNotFoundException =>
          logInfo("can't find file from" + cachePath)
          "None"
      }
      if (fileSize < 1024) {
        fileSize + " B"
      } else if (fileSize < 1024 * 1024) {
        fileSize / 1024 + " KB"
      } else if (fileSize < 1024 * 1024 * 1024) {
        fileSize / (1024 * 1024) + " MB"
      } else {
        fileSize / (1024 * 1024 * 1024) + " GB"
      }
    } else {
      "None"
    }
  }

  def getCubeDetailPageUrl(request: HttpServletRequest, cacheId: String): String = {
    "%s/Cube Management/Detail/?cacheId=%s"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheId)
  }
}
