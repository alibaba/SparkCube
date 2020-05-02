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

function buildCache() {
    const cacheId = document.getElementById('cacheId').innerHTML;
    const siteUrl = window.location.protocol + '//' + window.location.host;
    const saveMode = document.querySelector('input[name="saveModeRadio"]:checked').value;
    const optionalFilter = document.getElementById('optionalFilter').checked;

    const buildInfo = {};
    buildInfo.saveMode = saveMode;
    if (optionalFilter) {
      const columnSelector = document.getElementById('columnNameSelector');
      const columnName = columnSelector.options[columnSelector.selectedIndex].value;
      const valueTypeCached = document.querySelector('input[name="valueTypeRadio"]:checked').value;

      buildInfo.type = valueTypeCached;
      const data = {columnName: columnName};
      buildInfo.data = data;
      if (valueTypeCached == "fixed") {
        const fixedValues = document.getElementById('fixedValueInput').value;
        if (fixedValues != null && fixedValues.length != 0) {
          data.values = fixedValues;
        }
      } else {
        const rangeStart = document.getElementById('startValueInput').value;
        data.start = rangeStart;
        const rangeEnd = document.getElementById('endValueInput').value;
        data.end = rangeEnd;
        const dateTimeFormatChecked = document.getElementById('isDataTimeColumn').checked
        if (dateTimeFormatChecked) {
          const dateFormat = document.getElementById('withDateFormat').value;
          data.dateFormat = dateFormat;
        } else {
          data.dateFormat = '';
        }
      }
    } else {
      buildInfo.type = 'full';
      buildInfo.data = '';
    }

    const buildPath = "rcApi/v1/caches/" + cacheId + "/build";
    const url = location.protocol + '//' + location.host +
      location.pathname.replace(/(Cube%20Management\/Build)/, buildPath) +
      location.search;
    $.ajax({
      url: url,
      type: "PUT",
      contentType: "application/json",
      dataType: "json",
      data: JSON.stringify({param: JSON.stringify(buildInfo)}),
      headers: {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-HTTP-Method-Override": "PUT" },
      success: function(response) {
        if (response.status == 'SUCCEED') {
          displayModal("Build Cache",
            "Build job has submitted for " + cacheId + ", go back detail page to monitor" +
             " job status")
        } else {
          displayModal("Build Cache",
            "Failed to submit build job for " + cacheId + ".\n" + response.message)
        }
      }
    })
}

function periodBuildCache() {
  const cacheId = document.getElementById('cacheId').innerHTML;
  const siteUrl = window.location.protocol + '//' + window.location.host;
  const saveMode = document.querySelector('input[name="periodSaveModeRadio"]:checked').value;
  const startTime = document.getElementById('datetimepicker').querySelector('input').value;
  const triggerPeriod = document.getElementById('triggerPeriod').value;
  const triggerUnitNode = document.getElementById('triggerTimeUnitSelect');
  const triggerPeriodUnit = triggerUnitNode.options[triggerUnitNode.selectedIndex].value;
  const optionalStep = document.getElementById('optionalStep').checked;

  const periodBuildInfo = {}
  if (optionalStep) {
    const byTsColumns = document.getElementById('tsColRadio').checked;
    if (byTsColumns) {
      const columnNameSelector = document.getElementById('tsColumnName');
      const columnName = columnNameSelector.options[columnNameSelector.selectedIndex].value;
      periodBuildInfo.type = 'timestamp';
      periodBuildInfo.data = {
        colName: columnName,
        period: triggerPeriod,
        periodTimeUnit: triggerPeriodUnit,
        saveMode: saveMode,
        triggerTime: startTime
      }
    } else {
      const columnNameSelector = document.getElementById('dateTimeColumnName');
      const columnName = columnNameSelector.options[columnNameSelector.selectedIndex].value;
      const dateColFormat = document.getElementById('periodDateFormat').value;
      periodBuildInfo.type = 'datetime';
      periodBuildInfo.data = {
        colName: columnName,
        dateFormat: dateColFormat,
        period: triggerPeriod,
        periodTimeUnit: triggerPeriodUnit,
        saveMode: saveMode,
        triggerTime: startTime
      }
    }
  } else {
    periodBuildInfo.type = 'full';
    periodBuildInfo.data = {
      period: triggerPeriod,
      periodTimeUnit: triggerPeriodUnit,
      saveMode: saveMode,
      triggerTime: startTime
    }
  }

  const periodBuildPath = "rcApi/v1/caches/" + cacheId + "/periodBuild";
  const url = location.protocol + '//' + location.host +
    location.pathname.replace(/(Cube%20Management\/Build)/, periodBuildPath) +
    location.search;
  $.ajax({
    url: url,
    type: "PUT",
    contentType: "application/json",
    dataType: "json",
    data: JSON.stringify({param: JSON.stringify(periodBuildInfo)}),
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "X-HTTP-Method-Override": "PUT" },
    success: function(response) {
      if (response.status == 'SUCCEED') {
        displayModal("Timer Trigger Build Cache",
          "Timer Trigger build job has submitted for " + cacheId + ", go back detail page to" +
          " monitor job status")
      } else {
        displayModal("Timer Trigger Build Cache",
          "Failed to submit timer trigger build job for " + cacheId + ".\n" + response.message)
      }
    }
  })
}

$(function() {
  $("#optionalStep").change(function () {
    if (this.checked) {
      $('#stepDiv').removeClass('hide');
    } else {
      $('#stepDiv').addClass('hide');
    }
  });

  $("#isDataTimeColumn").change(function () {
    if (this.checked) {
      $('#buildDateTimeDiv').removeClass('hide');
    } else {
      $('#buildDateTimeDiv').addClass('hide');
    }
  });

  $("#optionalFilter").change(function () {
    if (this.checked) {
      $('#optionalFilterDiv').removeClass('hide');
    } else {
      $('#optionalFilterDiv').addClass('hide');
    }
  });

  $("#stepTypeSelector :input").change(function () {
    if (this.value == "timestamp") {
      $('#timestampDiv').removeClass('hide');
      $('#dateTimeDiv').addClass('hide');
    } else {
      $('#timestampDiv').addClass('hide');
      $('#dateTimeDiv').removeClass('hide');
    }
  });

  $("#valueTypeSelector :input").change(function () {
    if (this.value == "fixed") {
        $('#fixedDiv').removeClass('hide');
        $('#rangeDiv').addClass('hide');
    } else {
        $('#fixedDiv').addClass('hide');
        $('#rangeDiv').removeClass('hide');
    }
  });

  $('#datetimepicker').datetimepicker({
    format: 'yyyy-MM-dd hh:mm:ss'
  });

  const imagePath = "img/glyphicons-halflings.png";
  const imageUrl = location.protocol + '//' + location.host +
    location.pathname.replace(/(Cube%20Management\/Build)/, imagePath) +
    location.search;
  $('.icon-calendar').css('background-image', 'url(' + imageUrl + ')');
  $('.icon-time').css('background-image', 'url(' + imageUrl + ')');
})

function displayModal(title, bodyText) {
  $('#actionTitle').html(title)
  $('#actionBody').html(bodyText)
  $('#actionModal').modal('show')
}
