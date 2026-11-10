package com.example.data.model

import com.squareup.moshi.JsonClass
import java.util.UUID

@JsonClass(generateAdapter = true)
data class TaskItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val area: String = "work", // work, personal, home, study
    val pid: String = "",
    val due: String = "",
    val prio: String = "mid", // low, mid, high
    val st: String = "todo", // todo, doing, done
    val doneAt: String = "",
    val desc: String = ""
)

@JsonClass(generateAdapter = true)
data class ProjectItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val status: String = "plan", // plan, doing, hold, done
    val prio: String = "mid", // low, mid, high
    val start: String = "",
    val due: String = "",
    val budget: Double = 0.0,
    val link: String = "",
    val desc: String = ""
)

@JsonClass(generateAdapter = true)
data class TxItem(
    val id: String = UUID.randomUUID().toString(),
    val type: String = "out", // out, in
    val t: String = "",
    val cat: String = "",
    val amt: Double = 0.0,
    val d: String = "",
    val pid: String = "",
    val bid: String = "" // connected buy id if created from shopping list
)

@JsonClass(generateAdapter = true)
data class BuyItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val price: Double = 0.0,
    val kind: String = "want", // need, want, daily
    val prio: String = "mid", // low, mid, high
    val st: String = "want", // want, plan, bought, drop
    val link: String = "",
    val note: String = "",
    val d: String = "",
    val txid: String = ""
)

@JsonClass(generateAdapter = true)
data class KeyResultItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val done: Boolean = false
)

@JsonClass(generateAdapter = true)
data class GoalItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val cat: String = "personal", // personal, career, finance, health, learning, relation, other
    val hz: String = "mid", // short, mid, long
    val due: String = "",
    val st: String = "active", // active, done, hold, dropped
    val prog: Double = 0.0,
    val kpi: String = "",
    val desc: String = "",
    val pid: String = "",
    val kr: List<KeyResultItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ActionLogItem(
    val id: String = UUID.randomUUID().toString(),
    val d: String = "",
    val txt: String = ""
)

@JsonClass(generateAdapter = true)
data class IdeaItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val type: String = "idea", // idea, plan
    val st: String = "new", // new, review, approved, doing, done, hold, rejected
    val prio: String = "mid", // low, mid, high
    val pid: String = "",
    val desc: String = "",
    val prog: Double = 0.0,
    val log: List<ActionLogItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class HabitItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val log: List<String> = emptyList(), // list of completed "yyyy/MM/dd" dates
    val arch: Boolean = false
)

@JsonClass(generateAdapter = true)
data class WorkItem(
    val id: String = UUID.randomUUID().toString(),
    val d: String = "",
    val type: String = "پیاده‌روی",
    val min: Int = 30,
    val intVal: Int = 2, // 1: سبک, 2: متوسط, 3: سنگین
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class BookItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val author: String = "",
    val pages: Int = 0,
    val cur: Int = 0,
    val st: String = "want", // want, reading, done, pause
    val rate: Int = 0, // 0..5
    val note: String = "",
    val doneAt: String = ""
)

@JsonClass(generateAdapter = true)
data class ReadingLogItem(
    val id: String = UUID.randomUUID().toString(),
    val bid: String = "",
    val d: String = "",
    val pages: Int = 0,
    val min: Int = 0
)

@JsonClass(generateAdapter = true)
data class SkillItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val level: Int = 0, // 0..100
    val target: Int = 100, // 0..100
    val plan: String = "",
    val res: String = ""
)

@JsonClass(generateAdapter = true)
data class SkillPracticeItem(
    val id: String = UUID.randomUUID().toString(),
    val sid: String = "",
    val d: String = "",
    val min: Int = 0,
    val note: String = ""
)

@JsonClass(generateAdapter = true)
data class DailyJournalItem(
    val id: String = UUID.randomUUID().toString(),
    val d: String = "",
    val mood: Int = 0, // 1..5
    val gr: String = "", // gratitude
    val lesson: String = "",
    val txt: String = "" // free note
)

@JsonClass(generateAdapter = true)
data class SessionTopicItem(
    val id: String = UUID.randomUUID().toString(),
    val kind: String = "goal", // goal, idea, proj
    val ref: String = "",
    val verdict: String = "pending", // pending, approved, rejected, postponed
    val vnote: String = ""
)

@JsonClass(generateAdapter = true)
data class SessionResolutionItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val due: String = "",
    val st: String = "todo", // todo, done
    val tid: String = "" // linked task id
)

@JsonClass(generateAdapter = true)
data class ThinkingSessionItem(
    val id: String = UUID.randomUUID().toString(),
    val t: String = "",
    val d: String = "",
    val place: String = "",
    val kind: String = "week", // think, week, month, decide, idea
    val st: String = "plan", // plan, held
    val agenda: String = "",
    val notes: String = "",
    val items: List<SessionTopicItem> = emptyList(),
    val res: List<SessionResolutionItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class NotebookSettings(
    val budgets: Map<String, Double> = emptyMap()
)

@JsonClass(generateAdapter = true)
data class NotebookState(
    val set: NotebookSettings = NotebookSettings(),
    val proj: List<ProjectItem> = emptyList(),
    val task: List<TaskItem> = emptyList(),
    val tx: List<TxItem> = emptyList(),
    val buy: List<BuyItem> = emptyList(),
    val goal: List<GoalItem> = emptyList(),
    val idea: List<IdeaItem> = emptyList(),
    val book: List<BookItem> = emptyList(),
    val rlog: List<ReadingLogItem> = emptyList(),
    val work: List<WorkItem> = emptyList(),
    val skill: List<SkillItem> = emptyList(),
    val sprac: List<SkillPracticeItem> = emptyList(),
    val habit: List<HabitItem> = emptyList(),
    val jour: List<DailyJournalItem> = emptyList(),
    val sess: List<ThinkingSessionItem> = emptyList()
)
