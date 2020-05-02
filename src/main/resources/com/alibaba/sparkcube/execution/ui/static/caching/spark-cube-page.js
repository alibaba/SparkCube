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
    $("span.additional-dimension").click(function() {
        $(this).parent().find('input[type="checkbox"]').trigger('click');
    });

    $("#select-all-dimension").click(function() {
       var status = window.localStorage.getItem("select-all-dimension") == "true";
       status = !status;
       if (this.checked) {
          // Toggle all un-checked options.
          $('input[name="cacheField"][type="checkbox"]:not(:checked)').trigger('click');
       } else {
          // Toggle all checked options.
          $('input[name="cacheField"][type="checkbox"]:checked').trigger('click');
       }
       window.localStorage.setItem("select-all-dimension", "" + status);
    });

    $("#close-cache").click(function(){
       $("#mymodal-data").toggle();
       var url = window.location.protocol + '//' + window.location.host + window.location.pathname;
       window.location.replace(url);
    });

    $("#create-cache").click(function(){
       var cacheType = $.trim($('#cacheType').text());
       var db = $.trim($('#cacheDataBase').text());
       var table = $.trim($('#cacheTableName').text());
       var cacheFieldArray = [];
       $.each($('input[name="cacheField"]:checkbox:checked'),function(){
         cacheFieldArray.push($(this).val());
       });
       var cacheRewrite = $('#cacheRewrite option:selected').text()
       var cacheProvider = $('#cacheProvider option:selected').text()
       var cachePartitionColumnArray = [];
       $.each($('input[name="cachePartitionColumn"]:checkbox:checked'),function(){
         cachePartitionColumnArray.push($(this).val());
       });
       var ZOrderColumnArray = [];
       $.each($('input[name="cacheZOrderColumn"]:checkbox:checked'),function(){
         ZOrderColumnArray.push($(this).val());
       });
       var cache = $('#cacheName').val();
       const createPath = "rcApi/v1/caches/" + db + "." + table;
       var url = location.protocol + '//' +
        location.host +
        location.pathname.replace(/(Cube%20Management\/Create)/, createPath) +
        location.search;
       var cacheDetail = {
           cacheName: cache,
           rewriteEnabled: cacheRewrite,
           provider: cacheProvider,
           partitionColumns: cachePartitionColumnArray,
           zorderColumns: ZOrderColumnArray
       }
       var flag = false;
       var p = new RegExp('^[a-zA-Z][a-zA-Z0-9_-]{0,19}$');
       if (cacheName.length == 0 || !p.test(cache)) {
          alert("Invalid cache name")
       } else if (cacheFieldArray.length ==0) {
          alert("please choose field")
       } else if (!judgeSubset(cachePartitionColumnArray, cacheFieldArray)) {
          alert("the partition fields should within the scope of dimension fields")
       } else if (!judgeSubset(ZOrderColumnArray, cacheFieldArray)) {
          alert("the zOrder fields should within the scope of dimension fields")
       } else if (judgeIntersect(cachePartitionColumnArray, ZOrderColumnArray)) {
          alert("the partition and zOrder field can't be same")
       } else if (cacheType == "RawCache") {
          cacheDetail.type = "raw"
          cacheDetail.schema = {
            selectColumns: cacheFieldArray
          }
          flag = true;
       } else if (cacheType == 'CubeCache') {
           var measureFieldArray = []
           $.each($('select[name="measureField"] option:checked'),function(){
              measureFieldArray.push($(this).val());
           });
           var measureFunctionArray = []
           $.each($('select[name="measureFunction"] option:checked'),function(){
             measureFunctionArray.push($(this).val());
           });
           if (measureFieldArray.filter(v => v.length == 0).length > 0) {
             alert("please choose measure field");
           } else if (measureFunctionArray.filter(v => v.length == 0).length > 0) {
             alert("please choose measure function");
           } else {
             for (var i=0; i < measureFunctionArray.length; i++) {
                if (measureFunctionArray[i] == "AVERAGE") {
                  measureFunctionArray.pop(measureFunctionArray[i]);
                  var field = measureFieldArray[i];
                  measureFieldArray.pop(field);
                  measureFieldArray.push(field);
                  measureFieldArray.push(field);
                  measureFunctionArray.push("COUNT");
                  measureFunctionArray.push("SUM");
                }
             }
             cacheDetail.type = "cube"
             cacheDetail.schema = {
               dimensions: cacheFieldArray,
               measuresFiled: measureFieldArray,
               measuresFunction: measureFunctionArray
             }
             flag = true;
           }
       }

       if (flag == true) {
           $.ajax({
                url: url,
                type: "PUT",
                contentType: "application/json",
                dataType: "json",
                data: JSON.stringify({param: JSON.stringify(cacheDetail)}),
                headers: {
                    "Accept": "application/json",
                    "Content-Type": "application/json",
                    "X-HTTP-Method-Override": "PUT" },
                success: function(response) {
                    $('#mymodal-data').toggle();
                    if (response.status == 'SUCCEED') {
                      displayModal("Create Cache",
                      "Create cache " + table +" successfully")
                    } else if (response.status == 'FAILED'){
                      displayModal("Create Cube Management",
                      "Cache already exists for " + table)
                    } else {
                      displayModal("Create Cache",
                      response.message)
                    }
                }
           })
       }
    });
})

function judgeSubset(a, b) {
    for (var i=0; i<a.length; i++) {
        if (b.indexOf(a[i]) == -1) {
            return false;
        }
    }
    return true;
}

function judgeIntersect(a, b) {
    for (var i=0; i<a.length; i++) {
        if (b.indexOf(a[i]) != -1) {
            return true;
        }
    }
    return false;
}

function dropCache(cacheId) {
  const dropPath = "rcApi/v1/caches/" + cacheId + "/delete";
  const url = location.protocol + '//' + location.host +
    location.pathname.replace(/(Cube%20Management)/, dropPath) + location.search;
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
          displayModal("Drop Cache",
            "Succeed to drop Cache " + cacheId + ".");
        } else {
          displayModal("Drop Cache",
            "Failed to drop cache " + cacheId +
             ".\n" + response.message);
        }
      }
  })
}

function deleteBuildFilter() {
  const div = this.parentNode;
  div.parentNode.removeChild(div);
}

function insertAfter( newElement, targetElement ) {
  var parent = targetElement.parentNode;
  if( parent.lastChild == targetElement ){
    parent.appendChild(newElement);
  }else{
    parent.insertBefore(newElement, targetElement.nextSibling);
  }
}

function addMeasure() {
  var nodeAll = document.getElementsByClassName('additional-metrics example')
  var node = nodeAll[nodeAll.length - 1]
  insertAfter(node.cloneNode(true), node)
}

function removeClick(obj) {
  if ($(".additional-metrics.example").length == 1) {
    alert("one measure field and function at least")
  } else {
    obj.parentElement.remove()
  }
}

function displayModal(title, bodyText) {
  $('#actionTitle').html(title)
  $('#actionBody').html(bodyText)
  $('#actionModal').modal('show')
}
