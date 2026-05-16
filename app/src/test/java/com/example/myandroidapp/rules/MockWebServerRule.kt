package com.example.myandroidapp.rules

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class MockWebServerRule : TestRule {

    val server = MockWebServer()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                server.start()
                try {
                    base.evaluate()
                } finally {
                    server.shutdown()
                }
            }
        }
    }

    fun enqueueJson(code: Int, body: String) {
        server.enqueue(
            MockResponse()
                .setResponseCode(code)
                .setBody(body)
                .addHeader("Content-Type", "application/json"),
        )
    }

    fun baseUrl() = server.url("/").toString()
}
