/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the 'License'); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(function() {
    $('#rewriteToggle').change(function() {
      const enabled = $(this).prop('checked');
      const cacheId = document.getElementById('cacheId').innerHTML;
      const enablePath = 'rcApi/v1/caches/' + cacheId + '/enable';
      const url = location.protocol + '//' + location.host +
        location.pathname.replace(/(Cube%20Management\/Detail)/, enablePath) +
        location.search;
      const body = {param: enabled.toString()}
      $.ajax({
        url: url,
        type: "PUT",
        contentType: "application/json",
        dataType: "json",
        data: JSON.stringify(body),
        headers: {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "X-HTTP-Method-Override": "PUT" },
        success: function(response) {
            var setStr = ""
            if (enabled) {
              setStr = "enable"
            } else {
              setStr = "disable"
            }
            if (response.status == 'SUCCEED') {
              displayModal("Enable/Disable Query Rewrite",
               "Succeed to " + setStr + " query rewrite for " + cacheId + ".")
            } else {
              displayModal("Enable/Disable Query Rewrite",
                "Failed to " + setStr + " query rewrite for " + cacheId + ".\n" +
                 response.message)
            }
        }
      })
     });

    $("span.additional-dimension").click(function() {
        $(this).parent().find('input[type="checkbox"]').trigger('click');
    });

})

function displayModal(title, bodyText) {
  $('#actionTitle').html(title)
  $('#actionBody').html(bodyText)
  $('#actionModal').modal('show')
}

function clearBuildHistory() {
  const cacheId = document.getElementById('cacheId').innerHTML;
  const buildHistoryPath = "rcApi/v1/caches/" + cacheId + "/buildHistory/delete";
  const url = location.protocol + '//' + location.host +
    location.pathname.replace(/(Cube%20Management\/Detail)/, buildHistoryPath) +
    location.search;
  $.ajax({
    url: url,
    type: "PUT",
    contentType: "application/json",
    dataType: "json",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "X-HTTP-Method-Override": "PUT" },
      success: function(response) {
        if (response.status == 'SUCCEED') {
          displayModal("Clear Build History",
            "Succeed to clear build history for " + cacheId + ".")
        } else {
          displayModal("Enable/Disable Query Rewrite",
            "Failed to clear build history for " + cacheId + ".\n" + response.message)
        }
      }
  })
}

function cancelPeriodBuild() {
  const cacheId = document.getElementById('cacheId').innerHTML;
  const periodBuildPath = "rcApi/v1/caches/" + cacheId + "/periodBuild/delete"
  const url = location.protocol + '//' + location.host +
    location.pathname.replace(/(Cube%20Management\/Detail)/, periodBuildPath) +
    location.search;
  $.ajax({
    url: url,
    type: "PUT",
    contentType: "application/json",
    dataType: "json",
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "X-HTTP-Method-Override": "PUT" },
      success: function(response) {
        if (response.status == 'SUCCEED') {
          displayModal("Cancel Period Build Job",
            "Succeed to cancel period build job for " + cacheId + ".")
        } else {
          displayModal("Enable/Disable Query Rewrite",
            "Failed to cancel period build job for " + cacheId + ".\n" + response.message)
        }
      }
  })
}

function deleteCachePartition(pathToDelete) {
  const cacheId = document.getElementById('cacheId').innerHTML;
  const periodBuildPath = "rcApi/v1/caches/" + cacheId + "/partitions/delete";
  const url = location.protocol + '//' + location.host +
    location.pathname.replace(/(Cube%20Management\/Detail)/, periodBuildPath) +
    location.search;
  $.ajax({
    url: url,
    type: "PUT",
    contentType: "application/json",
    dataType: "json",
    data: JSON.stringify({param: pathToDelete}),
    headers: {
      "Accept": "application/json",
      "Content-Type": "application/json",
      "X-HTTP-Method-Override": "PUT" },
      success: function(response) {
        if (response.status == 'SUCCEED') {
          displayModal("Delete cache data partition",
            "Succeed to delete partition path:" + pathToDelete + " for " + cacheId + ".")
        } else {
          displayModal("Delete cache data partition",
            "Failed to delete partition path:" + pathToDelete + " for " + cacheId +
             ".\n" + response.message)
        }
      }
  })
}

$(document).ready(function() {
    $('#cachePartitionsListTable').DataTable();
    $('#cacheBuildHistoryTable').DataTable();
} )
