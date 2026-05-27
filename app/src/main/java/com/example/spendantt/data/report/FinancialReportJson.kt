package com.example.spendantt.data.report

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

object FinancialReportJson {

    fun encode(report: FinancialReport): String {
        val obj = JSONObject().apply {
            put("startDate", report.startDate.toString())
            put("endDate", report.endDate.toString())
            put("periodLabel", report.periodLabel)
            put("generatedAt", report.generatedAt)
            put("displayCurrency", report.displayCurrency)
            put("displayRate", report.displayRate)
            put("totalSpentCop", report.totalSpentCop)
            put("reportsGeneratedCount", report.reportsGeneratedCount)
            put("mostActiveWeekday", report.mostActiveWeekday ?: JSONObject.NULL)

            put("dailySpends", JSONArray().apply {
                report.dailySpends.forEach { ds ->
                    put(JSONObject().apply {
                        put("date", ds.date.toString())
                        put("amountCop", ds.amountCop)
                    })
                }
            })

            put("topExpenses", JSONArray().apply {
                report.topExpenses.forEach { te ->
                    put(JSONObject().apply {
                        put("name", te.name)
                        put("amountCop", te.amountCop)
                        put("category", te.category)
                        put("date", te.date.toString())
                    })
                }
            })

            put("topCategories", JSONArray().apply {
                report.topCategories.forEach { ct ->
                    put(JSONObject().apply {
                        put("label", ct.label)
                        put("amountCop", ct.amountCop)
                        put("count", ct.count)
                    })
                }
            })

            put("bqInsights", JSONArray(report.bqInsights))
        }
        return obj.toString()
    }

    fun decode(json: String): FinancialReport {
        val obj = JSONObject(json)
        val dailySpends = mutableListOf<DailySpend>()
        val dailyArr = obj.optJSONArray("dailySpends") ?: JSONArray()
        for (i in 0 until dailyArr.length()) {
            val item = dailyArr.getJSONObject(i)
            dailySpends.add(
                DailySpend(
                    date = LocalDate.parse(item.getString("date")),
                    amountCop = item.getDouble("amountCop"),
                )
            )
        }

        val topExpenses = mutableListOf<TopExpense>()
        val expensesArr = obj.optJSONArray("topExpenses") ?: JSONArray()
        for (i in 0 until expensesArr.length()) {
            val item = expensesArr.getJSONObject(i)
            topExpenses.add(
                TopExpense(
                    name = item.getString("name"),
                    amountCop = item.getDouble("amountCop"),
                    category = item.getString("category"),
                    date = LocalDate.parse(item.getString("date")),
                )
            )
        }

        val topCategories = mutableListOf<CategoryTotal>()
        val catsArr = obj.optJSONArray("topCategories") ?: JSONArray()
        for (i in 0 until catsArr.length()) {
            val item = catsArr.getJSONObject(i)
            topCategories.add(
                CategoryTotal(
                    label = item.getString("label"),
                    amountCop = item.getDouble("amountCop"),
                    count = item.getInt("count"),
                )
            )
        }

        val insights = mutableListOf<String>()
        val insightsArr = obj.optJSONArray("bqInsights") ?: JSONArray()
        for (i in 0 until insightsArr.length()) insights.add(insightsArr.getString(i))

        val mostActiveWeekday = if (obj.isNull("mostActiveWeekday")) null else obj.getInt("mostActiveWeekday")

        return FinancialReport(
            startDate = LocalDate.parse(obj.getString("startDate")),
            endDate = LocalDate.parse(obj.getString("endDate")),
            periodLabel = obj.getString("periodLabel"),
            generatedAt = obj.getLong("generatedAt"),
            displayCurrency = obj.getString("displayCurrency"),
            displayRate = obj.getDouble("displayRate"),
            totalSpentCop = obj.getDouble("totalSpentCop"),
            dailySpends = dailySpends,
            topExpenses = topExpenses,
            topCategories = topCategories,
            reportsGeneratedCount = obj.getInt("reportsGeneratedCount"),
            mostActiveWeekday = mostActiveWeekday,
            bqInsights = insights,
        )
    }
}
