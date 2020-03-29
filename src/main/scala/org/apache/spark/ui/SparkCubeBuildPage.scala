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

import org.apache.spark.sql.catalyst.TableIdentifier

import com.alibaba.sparkcube.optimizer.CacheIdentifier

// UIUtils is private to package spark
class SparkCubeBuildPage(parent: SparkCubeTab)
  extends AbstractSparkCubePage(parent, "Build") {

  override def render(request: HttpServletRequest): Seq[Node] = {
    val parameterCacheId = UIUtils.stripXSS(request.getParameter("cacheId"))
    require(parameterCacheId != null && parameterCacheId.nonEmpty, "Missing cache id parameter")
    val action = UIUtils.stripXSS(request.getParameter("action"))
    val cacheId = CacheIdentifier(parameterCacheId)

    val fields = sparkSession.sessionState.catalog
      .getTableMetadata(TableIdentifier(cacheId.viewName, Some(cacheId.db)))
      .schema
      .fields
      .map(_.name)

    val resources = generateLoadResources(request) :+
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
              <a href={getCubeDetailPageUrl(request, cacheId.toString)} class="btn btn-primary">
                Close
              </a>
            </div>
          </div>
        </div>
      </div>

    val (title, content) = action match {
      case "build" =>
        ("Cube Management Build Page",
          resources ++ buildNode(fields) :+ hidenCacheId(cacheId.toString))
      case "trigger" =>
        ("Cube Management Timer Trigger Build Page",
          resources ++ periodBuildNode(fields) :+ hidenCacheId(cacheId.toString))
    }

    UIUtils.headerSparkPage(request, title, content, parent)
  }

  private def hidenCacheId(cacheId: String): Node = {
    <p id="cacheId" hidden="true" value={cacheId}>{cacheId}</p>
  }

  private def buildNode(fields: Array[String]): Seq[Node] = {
    <form class="form-horizontal">
      <div class="control-group">
        <label class="control-label">Save Mode:</label>
        <div class="controls">
          <label class="radio" for="fullBuildRadio">Overwrite
            <input type="radio" name="saveModeRadio" value="Overwrite" checked="true"/>
          </label>
          <label class="radio" for="incBuildRadio">Append
            <input type="radio" name="saveModeRadio" value="Append"/>
          </label>
        </div>
      </div>
      <div class="control-group">
        <label class="control-label">Optional Filter</label>
        <div class="controls">
          <input type="checkbox" id="optionalFilter"/>
        </div>
      </div>
      <div id="optionalFilterDiv" class="hide">
        <div class="control-group">
          <label class="control-label" for="columnNameSelector">Column:</label>
          <div class="controls">
            <select id="columnNameSelector" required="true">
              {fields.map(field => <option value={field}>
              {field}
            </option>)}
            </select>
          </div>
        </div>
        <div class="control-group">
          <label class="control-label">Filter Type:</label>
          <div class="controls" id="valueTypeSelector">
            <label class="radio" for="fixedValueRadio">Fixed Values
              <input type="radio" name="valueTypeRadio" id="fixedValueRadio" value="fixed"
                     checked="true"/>
            </label>
            <label class="radio" for="rangeValueRadio">Range Values
              <input type="radio" name="valueTypeRadio" id="rangeValueRadio" value="range"/>
            </label>
          </div>
        </div>
        <div class="control-group" id="fixedDiv">
          <label class="control-label" for="fixedValueInput">Values:</label>
          <div class="controls">
            <input id="fixedValueInput" type="text" required="true"
                   placeholder="value1,value2,value3...">
            </input>
            <small class="form-text text-muted">
              Filter view data with column value equals to input values.
            </small>
          </div>
        </div>
        <div id="rangeDiv" class="hide">
          <div class="control-group">
            <label class="control-label" for="startValueInput">Start:</label>
            <div class="controls">
              <input name="startInput" id="startValueInput" required="true"
                     type="text">
              </input>
              <small class="form-text text-muted">
                Required, inclusive in range.
              </small>
            </div>
          </div>
          <div class="control-group">
            <label class="control-label" for="endValueInput">End:</label>
            <div class="controls">
              <input class="span2" name="endInput" id="endValueInput" type="text">
              </input>
              <small class="form-text text-muted">
                Nullable, exclusive in range.
              </small>
            </div>
          </div>
          <div class="control-group">
            <label class="control-label">DateTime Column</label>
            <div class="controls">
              <input type="checkbox" id="isDataTimeColumn"/>
            </div>
          </div>
          <div class="control-group hide" id="buildDateTimeDiv">
            <label class="control-label" for="withDateFormat">DateTime Format:</label>
            <div class="controls">
              <input id="withDateFormat" required="true"
                     type="text" placeholder="Such as 'yyyy-MM-dd HH:mm:ss'"></input>
              <small class="form-text text-muted">
                If available, filter would compare column as TimestampType.
              </small>
            </div>
          </div>
        </div>
      </div>
      <div class="control-group">
        <div class="controls">
          <button type="button" class="btn" onclick="buildCache()">Submit</button>
        </div>
      </div>
    </form>
  }

  private def periodBuildNode(fields: Array[String]): Seq[Node] = {
    <form class="form-horizontal">
      <div class="control-group">
        <label class="control-label">Save Mode:</label>
        <div class="controls">
          <label class="radio" for="periodOverwriteRadio">Overwrite
            <input type="radio" id="periodOverwriteRadio" name="periodSaveModeRadio"
                   value="Overwrite" checked="true"/>
          </label>
          <label class="radio" for="periodAppendRadio">Append
            <input type="radio" id="periodAppendRadio" name="periodSaveModeRadio" value="Append"/>
          </label>
        </div>
      </div>
      <div class="control-group">
        <label class="control-label">Trigger Strategy:</label>
      </div>
      <div class="control-group">
        <label class="control-label" for="datetimepicker">Start At:</label>
        <div class="controls">
          <div id="datetimepicker" class="input-append date">
            <input type="text" required="true"></input>
            <span class="add-on">
              <i data-time-icon="icon-time" data-date-icon="icon-calendar"></i>
            </span>
          </div>
        </div>
      </div>
      <div class="control-group">
        <label class="control-label" for="triggerPeriod">Period:</label>
        <div class="controls">
          <input type="text" id="triggerPeriod" required="true"/>
          <select id="triggerTimeUnitSelect" required="true">
            <option value="DAYS">Day</option>
            <option value="HOURS">Hour</option>
            <option value="MINUTES">Minute</option>
            <option value="SECONDS">Second</option>
          </select>
        </div>
      </div>
      <div class="control-group">
        <label class="control-label">Optional Step:</label>
        <div class="controls">
          <input type="checkbox" id="optionalStep"/>
        </div>
      </div>
      <div id="stepDiv" class="hide">
        <div class="control-group">
          <label class="control-label">Step By:</label>
          <div class="controls" id="stepTypeSelector">
            <label class="radio" for="tsColRadio">TimeStamp Column(Long Type)
              <input type="radio" name="stepTypeRadio" id="tsColRadio" value="timestamp"
                     checked="true"/>
            </label>
            <label class="radio" for="datetimeColRadio">DateTime Column(String Type)
              <input type="radio" name="stepTypeRadio" id="datetimeColRadio" value="datetime"/>
            </label>
          </div>
        </div>
        <div class="control-group" id="timestampDiv">
          <label class="control-label" for="tsColumnName">Column Name:</label>
          <div class="controls">
            <select class="form-control" id="tsColumnName" required="true">
              {fields.map(field => <option value={field}>
              {field}
            </option>)}
            </select>
          </div>
        </div>
        <div id="dateTimeDiv" class="hide">
          <div class="control-group">
            <label class="control-label" for="dateTimeColumnName">Column Name:</label>
            <div class="controls">
              <select class="form-control" id="dateTimeColumnName" required="true">
                {fields.map(field => <option value={field}>
                {field}
              </option>)}
              </select>
            </div>
          </div>
          <div class="control-group">
            <label class="control-label" for="withPeriodDateFormat">
              DateTime Format
            </label>
            <div class="controls">
              <input class="span2" hide="true" id="periodDateFormat" required="true"
                     type="text" placeholder="Such as 'yyyy-MM-dd HH:mm:ss'"></input>
              <small class="form-text text-muted">
                If available, filter would compare column as TimestampType value.
              </small>
            </div>
          </div>
        </div>
      </div>
      <div class="control-group">
        <div class="controls">
          <button type="button" class="btn" onclick="periodBuildCache()">Submit</button>
        </div>
      </div>
    </form>
  }

  private def generateLoadResources(request: HttpServletRequest): Seq[Node] = {
    // scalastyle:off
      <link rel="stylesheet" href={UIUtils.prependBaseUri(request,
      "/static/caching/bootstrap-datetimepicker.min.css")} type="text/css"/>
      <script src={UIUtils.prependBaseUri(request, "/static/contrib/bootstrap.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request,
        "/static/caching/bootstrap-datetimepicker.min.js")}></script>
      <script src={UIUtils.prependBaseUri(request,
        "/static/caching/spark-cube-build-page.js")}></script>
    // scalastyle:on
  }
}
