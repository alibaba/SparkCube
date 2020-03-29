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

// UIUtils is private to package spark
case class TableOrView(
    tableId: Int,
    dataBase: String,
    tableName: String,
    fields: Array[String],
    rawCacheUrl: String,
    cubeCacheUrl: String)

class SparkCubeCreatePage(parent: SparkCubeTab)
  extends AbstractSparkCubePage(parent, "Create") {

  override def render(request: HttpServletRequest): Seq[Node] = {
    val cachemanager = parent.sharedState.cubeManager
    val caches = cachemanager.listAllCaches(sparkSession)
    val catalog = sparkSession.sessionState.catalog
    var tableAndViews = Seq[TableOrView]()
    val dbs = catalog.listDatabases()
    var Index: Int = 0
    for (db <- dbs) {
      val tables = catalog.listTables(db)
      for(tableId <- tables) {
        val tableName = tableId.table
        val fields = catalog.getTableMetadata(tableId).schema.fields.map(field => field.name)
        tableAndViews = tableAndViews :+
          TableOrView(Index, db, tableName, fields, createCacheURL(request, "RawCache", Index),
            createCacheURL(request, "CubeCache", Index))
        Index += 1
      }
    }
    var content = tableOrViewUI(tableAndViews) ++
      generateLoadResources(request) ++
      checkCacheAction(request, tableAndViews) ++
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
              <a href={"%s/Cube Management/Create"
                .format(UIUtils.prependBaseUri(request, parent.basePath))} class="btn btn-primary">
                Close
              </a>
            </div>
          </div>
        </div>
      </div>

    UIUtils.headerSparkPage(request, "Table and View Information", content, parent)
  }



  private def checkCacheAction(
      request: HttpServletRequest,
      tableOrView: Seq[TableOrView]): Seq[Node] = {
    val cacheType = UIUtils.stripXSS(request.getParameter("cacheType"))
    val createCache = UIUtils.stripXSS(request.getParameter("createCache"))
    if (cacheType == null || createCache != null) {
      Nil
    } else {
      var popTitle: String = null
      val tableId = UIUtils.stripXSS(request.getParameter("tableId")).toInt
      val db = tableOrView(tableId).dataBase
      val table = tableOrView(tableId).tableName
      val fields = tableOrView(tableId).fields
      if ("RawCache".equals(cacheType)) {
        rawCachePopWindow(request, cacheType, db, table, fields, "column selector", false)
      } else if ("CubeCache".equals(cacheType)) {
        rawCachePopWindow(request, cacheType, db, table, fields, "dimension selector", true)
      } else {
        Nil
      }
    }
  }

  private def measureDiv(fields: Array[String]): Seq[Node] = {
    <div>
      <span class="expand-additional-metrics">
        <span class="expand-additional-metrics-arrow arrow-open"></span>
        <a>Measure selector</a>
      </span>
      <div class="additional-metrics example" id="addMeasureColumn">
        <select name="measureField">
          <option></option>
          {fields.map(field => <option>{field}</option>)}
        </select>
        <select name="measureFunction">
          <option></option>
          <option>COUNT</option>
          <option>SUM</option>
          <option>MAX</option>
          <option>MIN</option>
          <option>AVERAGE</option>
          <option>PRE_COUNT_DISTINCT</option>
          <option>PRE_APPROX_COUNT_DISTINCT</option>
        </select>
        <button class="btn btn-info btn-mini" id="" onclick="addMeasure()">+</button>
        <button class="btn btn-info btn-mini" id="" onclick="removeClick(this)">-</button>
      </div>
    </div>
  }

  private def rawCachePopWindow(request: HttpServletRequest, popTitle: String, db: String,
    table: String, fields: Array[String], columnName: String, IsCube: Boolean): Seq[Node] = {
    <div class="modal" id="mymodal-data"
         tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel" aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <div class="modal-header">
            <h4 class="modal-title" id="cacheType">{popTitle}</h4>
          </div>
          <div class="modal-body">
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>database</a>
              </span>
              <div class="additional-metrics">
                <label id="cacheDataBase">
                  {db}
                </label>
              </div>
            </div>
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>table</a>
              </span>
              <div class="additional-metrics">
                <label id="cacheTableName">
                  {table}
                </label>
              </div>
            </div>
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>Cache Name</a>
              </span>
              <div class="additional-metrics">
                <label>
                  <input class="form-control input-sm" id="cacheName"
                         placeholder="" aria-controls="active-executors-table" />
                </label>
              </div>
            </div>
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>{columnName}</a>
              </span>
              <div class="additional-metrics">
                <ul>
                  <li>
                    <span>
                      <input type="checkbox" id="select-all-dimension"/>
                      <span class="additional-dimension"><em>(De)select All</em></span>
                    </span>
                  </li>
                  {fields.map(field => <li><span>
                  <input type="checkbox" name="cacheField" value={field}>
                    <span class="additional-dimension"><em>{field}</em></span></input></span></li>)}
                </ul>
              </div>
            </div>
            {if (IsCube) measureDiv(fields)}
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>rewrite</a>
              </span>
              <div class="additional-metrics">
                <select id="cacheRewrite">
                  <option>ENABLE</option>
                  <option>DISABLE</option>
                </select>
              </div>
            </div>
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>provider</a>
              </span>
              <div class="additional-metrics">
                <select id="cacheProvider">
                  <option>PARQUET</option>
                  <option>TEXT</option>
                  <option>JSON</option>
                  <option>JDBC</option>
                  <option>ORC</option>
                  <option>HIVE</option>
                </select>
              </div>
            </div>
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>partitionColumns</a>
              </span>
              <div class="additional-metrics">
                <ul>
                  {fields.map(field => <li><span><input type="checkbox"
                                                        name="cachePartitionColumn" value={field}>
                  <span class="additional-dimension"><em>{field}</em></span></input></span></li>)}
                </ul>
              </div>
            </div>
            <div>
              <span class="expand-additional-metrics">
                <span class="expand-additional-metrics-arrow arrow-open"></span>
                <a>ZOrderColumns</a>
              </span>
              <div class="additional-metrics">
                <ul>
                  {fields.map(field => <li><input type="checkbox" name="cacheZOrderColumn"
                                                  value={field}>
                  <span class="additional-dimension"><em>{field}</em></span></input></li>)}
                </ul>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <a href={"%s/Cube Management/Create"
              .format(UIUtils.prependBaseUri(request, parent.basePath))} class="btn">
              Cancle</a>
            <button type="button" class="btn btn-primary" id ="create-cache">OK</button>
          </div>
        </div>
      </div>
    </div>
  }

  private def createCacheURL(request: HttpServletRequest, cacheType: String, Index: Int): String =
    "%s/Cube Management/Create/?cacheType=%s&tableId=%s"
      .format(UIUtils.prependBaseUri(request, parent.basePath), cacheType, Index)

  private val tableAndViewHeader = Seq("dataBase", "tableName", "action")

  private def createRow(row: TableOrView): Seq[Node] = {
    // scalastyle:off
    <tr>
      <td>{row.dataBase}</td>
      <td>{row.tableName}</td>
      <td>
        <div><a href={row.rawCacheUrl}>raw cache</a></div>
        <div><a href={row.cubeCacheUrl}>cube cache</a></div>
      </td>
    </tr>
    // scalastyle:on
  }

  private def tableOrViewUI(tableOrView: Seq[TableOrView]): Seq[Node] = {
    if (tableOrView.isEmpty) {
      Nil
    } else {
      <div>
        {UIUtils.listingTable(tableAndViewHeader, createRow, tableOrView, id = Some(""))}
      </div>
    }
  }

  private def generateLoadResources(request: HttpServletRequest): Seq[Node] = {
    // scalastyle:off
      <link rel="stylesheet" href={UIUtils.prependBaseUri(request,
      "/static/caching/spark-cube-page.css")} type="text/css"/>
      <script src={UIUtils.prependBaseUri(request, "/static/contrib/bootstrap.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request,
      "/static/caching/spark-cube-page.js")}></script>
    // scalastyle:on
  }
}