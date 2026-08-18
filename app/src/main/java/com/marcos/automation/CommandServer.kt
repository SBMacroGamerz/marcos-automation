package com.marcos.automation

import fi.iki.elonen.NanoHTTPD
import org.json.JSONObject
import java.util.concurrent.CountDownLatch

class CommandServer(
    port: Int,
    private val service: MarcosAccessibilityService
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {

        val corsHeaders = mapOf(
            "Access-Control-Allow-Origin" to "*",
            "Access-Control-Allow-Methods" to "GET, POST, OPTIONS",
            "Access-Control-Allow-Headers" to "Content-Type"
        )

        if (session.method == Method.OPTIONS) {
            val resp = newFixedLengthResponse(
                Response.Status.OK,
                "text/plain",
                ""
            )

            corsHeaders.forEach { (k, v) ->
                resp.addHeader(k, v)
            }

            return resp
        }

        val response = when {
            session.uri == "/screen" &&
                session.method == Method.GET ->
                handleReadScreen()

            session.uri == "/tap" &&
                session.method == Method.POST ->
                handleTap(session)

            session.uri == "/tap-text" &&
                session.method == Method.POST ->
                handleTapText(session)

            session.uri == "/" ->
                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    JSONObject()
                        .put("status", "ok")
                        .put("service", "marcos-automation")
                        .toString()
                )

            else ->
                newFixedLengthResponse(
                    Response.Status.NOT_FOUND,
                    "text/plain",
                    "not found"
                )
        }

        corsHeaders.forEach { (k, v) ->
            response.addHeader(k, v)
        }

        return response
    }

    private fun handleReadScreen(): Response {
        val elements = service.readScreen()

        val body = JSONObject()
            .put("elements", elements)
            .toString()

        return newFixedLengthResponse(
            Response.Status.OK,
            "application/json",
            body
        )
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"] ?: ""
    }

    private fun handleTap(session: IHTTPSession): Response {
        return try {

            val json = JSONObject(readBody(session))
            val x = json.getDouble("x").toFloat()
            val y = json.getDouble("y").toFloat()

            val latch = CountDownLatch(1)
            var success = false

            service.tapAt(x, y) { result ->
                success = result
                latch.countDown()
            }

            latch.await()

            val body = JSONObject()
                .put("success", success)
                .toString()

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                body
            )

        } catch (e: Exception) {

            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                JSONObject()
                    .put("error", e.message)
                    .toString()
            )
        }
    }

    private fun handleTapText(session: IHTTPSession): Response {
        return try {

            val json = JSONObject(readBody(session))
            val text = json.getString("text")

            val latch = CountDownLatch(1)
            var success = false

            service.tapNodeContainingText(text) { result ->
                success = result
                latch.countDown()
            }

            latch.await()

            val body = JSONObject()
                .put("success", success)
                .toString()

            newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                body
            )

        } catch (e: Exception) {

            newFixedLengthResponse(
                Response.Status.BAD_REQUEST,
                "application/json",
                JSONObject()
                    .put("error", e.message)
                    .toString()
            )
        }
    }
}
