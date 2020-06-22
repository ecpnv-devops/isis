/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.apache.isis.client.kroviz.utils

import org.w3c.dom.Document
import org.w3c.dom.parsing.DOMParser
import org.w3c.dom.svg.SVGSVGElement

enum class Direction(val id: String) {
    UP("UP"),
    DOWN("DOWN")
}

class ScalableVectorGraphic(val data: String) {

    var document: Document = DOMParser().parseFromString(data, "image/svg+xml")
    private var root: SVGSVGElement
    lateinit var viewBox: ViewBox
    private var scale: Double = 1.0
    private val defaultFactor = 0.1
    var x: Double = 0.0
    var y: Double = 0.0

    init {
        root = document.rootElement!!
        initViewBox()
        initScale()
    }

    private fun initViewBox() {
        val raw = root.getAttribute("viewBox") as String
        val arr = raw.split(" ")
        viewBox = ViewBox(arr[0].toInt(), arr[1].toInt(), arr[2].toInt(), arr[3].toInt())
    }

    private fun initScale() {
        if (root.hasAttribute("transform")) {
            val scaleStr = root.getAttribute("transform")!!
            val arr1 = scaleStr.split(")")
            val arr2 = arr1[1].split("(")
            scale = arr2[1].toDouble()
        }
    }

    fun scaleUp(factor: Double = defaultFactor) {
        val s = scale + factor
        // the bigger img gets, the bigger x,y must be
        val w2 = viewBox.width / 2
        val h2 = viewBox.height / 2
        x = w2 * s - w2
        y = h2 * s - h2
        // svg origin is center (50% 50%) - x,y adjust it to top left
        // translate needs to 'set' be before scale
        root.setAttribute("transform", "translate($x $y) scale($s)")
    }

    fun scaleDown(factor: Double = defaultFactor) {
        scaleUp(factor * -1)
    }

    class ViewBox(val x: Int, val y: Int, var width: Int, var height: Int)

}
