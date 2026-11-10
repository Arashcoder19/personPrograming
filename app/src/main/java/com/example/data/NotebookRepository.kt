package com.example.data

import android.content.Context
import com.example.data.model.*
import com.example.util.JalaliCalendar
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class NotebookRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(NotebookState::class.java)

    private val file = File(context.filesDir, "notebook_state.json")

    private val _state = MutableStateFlow(NotebookState())
    val state: StateFlow<NotebookState> = _state.asStateFlow()

    init {
        loadInitialState()
    }

    private fun loadInitialState() {
        try {
            if (file.exists()) {
                val json = file.readText()
                val loaded = adapter.fromJson(json)
                if (loaded != null) {
                    _state.value = loaded
                    return
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        // If file doesn't exist or loading failed, initialize with default seed data
        val initial = createDefaultSeedData()
        _state.value = initial
        saveState(initial)
    }

    private fun saveState(newState: NotebookState) {
        _state.value = newState
        try {
            val json = adapter.indent("  ").toJson(newState)
            file.writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun exportJson(): String = withContext(Dispatchers.IO) {
        adapter.indent("  ").toJson(_state.value)
    }

    suspend fun importJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val imported = adapter.fromJson(json) ?: return@withContext false
            saveState(imported)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- Task operations ---
    fun addTask(title: String, area: String, pid: String, due: String, prio: String, desc: String = "") {
        val newTask = TaskItem(
            id = UUID.randomUUID().toString(),
            t = title,
            area = area,
            pid = pid,
            due = due,
            prio = prio,
            st = "todo",
            desc = desc
        )
        saveState(_state.value.copy(task = listOf(newTask) + _state.value.task))
    }

    fun addQuickTaskToday(title: String, area: String) {
        val newTask = TaskItem(
            id = UUID.randomUUID().toString(),
            t = title,
            area = area,
            pid = "",
            due = JalaliCalendar.today(),
            prio = "mid",
            st = "todo"
        )
        saveState(_state.value.copy(task = listOf(newTask) + _state.value.task))
    }

    fun toggleTask(taskId: String) {
        val today = JalaliCalendar.today()
        val updated = _state.value.task.map { task ->
            if (task.id == taskId) {
                val nextSt = if (task.st == "done") "todo" else "done"
                val doneAt = if (nextSt == "done") today else ""
                task.copy(st = nextSt, doneAt = doneAt)
            } else task
        }
        saveState(_state.value.copy(task = updated))
    }

    fun updateTask(updatedTask: TaskItem) {
        val today = JalaliCalendar.today()
        val processed = if (updatedTask.st == "done" && updatedTask.doneAt.isBlank()) {
            updatedTask.copy(doneAt = today)
        } else if (updatedTask.st != "done") {
            updatedTask.copy(doneAt = "")
        } else updatedTask

        val list = _state.value.task.map { if (it.id == processed.id) processed else it }
        saveState(_state.value.copy(task = list))
    }

    fun deleteTask(taskId: String) {
        val list = _state.value.task.filterNot { it.id == taskId }
        // Also remove tid link in resolutions
        val sessList = _state.value.sess.map { s ->
            s.copy(res = s.res.map { r -> if (r.tid == taskId) r.copy(tid = "") else r })
        }
        saveState(_state.value.copy(task = list, sess = sessList))
    }

    // --- Project operations ---
    fun addProject(name: String, budget: Double = 0.0, due: String = "", link: String = "", desc: String = "") {
        val newProj = ProjectItem(
            id = UUID.randomUUID().toString(),
            name = name,
            status = "plan",
            prio = "mid",
            start = JalaliCalendar.today(),
            due = due,
            budget = budget,
            link = link,
            desc = desc
        )
        saveState(_state.value.copy(proj = listOf(newProj) + _state.value.proj))
    }

    fun updateProject(updatedProj: ProjectItem) {
        val list = _state.value.proj.map { if (it.id == updatedProj.id) updatedProj else it }
        saveState(_state.value.copy(proj = list))
    }

    fun deleteProject(projId: String) {
        val projList = _state.value.proj.filterNot { it.id == projId }
        val taskList = _state.value.task.map { if (it.pid == projId) it.copy(pid = "") else it }
        val txList = _state.value.tx.map { if (it.pid == projId) it.copy(pid = "") else it }
        val goalList = _state.value.goal.map { if (it.pid == projId) it.copy(pid = "") else it }
        val ideaList = _state.value.idea.map { if (it.pid == projId) it.copy(pid = "") else it }
        val sessList = _state.value.sess.map { s ->
            s.copy(items = s.items.filterNot { it.kind == "proj" && it.ref == projId })
        }
        saveState(_state.value.copy(
            proj = projList,
            task = taskList,
            tx = txList,
            goal = goalList,
            idea = ideaList,
            sess = sessList
        ))
    }

    // --- Finance operations ---
    fun addTransaction(type: String, title: String, cat: String, amt: Double, date: String, pid: String = "", bid: String = "") {
        val newTx = TxItem(
            id = UUID.randomUUID().toString(),
            type = type,
            t = title,
            cat = cat,
            amt = amt,
            d = date,
            pid = pid,
            bid = bid
        )
        saveState(_state.value.copy(tx = listOf(newTx) + _state.value.tx))
    }

    fun updateTransaction(updatedTx: TxItem) {
        val list = _state.value.tx.map { if (it.id == updatedTx.id) updatedTx else it }
        // Sync with buy item if connected
        var buyList = _state.value.buy
        if (updatedTx.bid.isNotBlank()) {
            buyList = buyList.map { b ->
                if (b.id == updatedTx.bid) {
                    b.copy(t = updatedTx.t, price = updatedTx.amt, d = updatedTx.d)
                } else b
            }
        }
        saveState(_state.value.copy(tx = list, buy = buyList))
    }

    fun deleteTransaction(txId: String) {
        val tx = _state.value.tx.find { it.id == txId }
        val list = _state.value.tx.filterNot { it.id == txId }
        var buyList = _state.value.buy
        if (tx != null && tx.bid.isNotBlank()) {
            buyList = buyList.map { b ->
                if (b.id == tx.bid) b.copy(txid = "") else b
            }
        }
        saveState(_state.value.copy(tx = list, buy = buyList))
    }

    fun setBudgetLimit(cat: String, amount: Double) {
        val newBudgets = _state.value.set.budgets.toMutableMap()
        if (amount > 0) {
            newBudgets[cat] = amount
        } else {
            newBudgets.remove(cat)
        }
        saveState(_state.value.copy(set = _state.value.set.copy(budgets = newBudgets)))
    }

    // --- Shopping list operations ---
    fun addBuyItem(title: String, price: Double, kind: String, prio: String, link: String = "", note: String = "") {
        val item = BuyItem(
            id = UUID.randomUUID().toString(),
            t = title,
            price = price,
            kind = kind,
            prio = prio,
            st = "want",
            link = link,
            note = note
        )
        saveState(_state.value.copy(buy = listOf(item) + _state.value.buy))
    }

    fun markBuyItemBought(buyId: String): Boolean {
        val item = _state.value.buy.find { it.id == buyId } ?: return false
        val today = JalaliCalendar.today()
        val updatedItem = item.copy(st = "bought", d = today)
        var txList = _state.value.tx

        var createdTx = false
        if (item.price > 0) {
            val existingTx = txList.find { it.id == item.txid }
            if (existingTx != null) {
                txList = txList.map { if (it.id == existingTx.id) it.copy(t = item.t, amt = item.price, d = today) else it }
            } else {
                val newTx = TxItem(
                    id = UUID.randomUUID().toString(),
                    type = "out",
                    t = item.t,
                    cat = "خرید",
                    amt = item.price,
                    d = today,
                    bid = item.id
                )
                txList = listOf(newTx) + txList
                createdTx = true
            }
        }
        val buyList = _state.value.buy.map { if (it.id == buyId) updatedItem else it }
        saveState(_state.value.copy(buy = buyList, tx = txList))
        return createdTx
    }

    fun updateBuyItem(updatedBuy: BuyItem) {
        val list = _state.value.buy.map { if (it.id == updatedBuy.id) updatedBuy else it }
        // Sync with tx if bought
        var txList = _state.value.tx
        if (updatedBuy.st == "bought" && updatedBuy.price > 0) {
            val existingTx = txList.find { it.id == updatedBuy.txid }
            if (existingTx != null) {
                txList = txList.map { if (it.id == existingTx.id) it.copy(t = updatedBuy.t, amt = updatedBuy.price, d = updatedBuy.d) else it }
            }
        }
        saveState(_state.value.copy(buy = list, tx = txList))
    }

    fun deleteBuyItem(buyId: String) {
        val buy = _state.value.buy.find { it.id == buyId }
        val buyList = _state.value.buy.filterNot { it.id == buyId }
        var txList = _state.value.tx
        if (buy != null && buy.txid.isNotBlank()) {
            txList = txList.filterNot { it.id == buy.txid }
        }
        saveState(_state.value.copy(buy = buyList, tx = txList))
    }

    // --- Goal operations ---
    fun addGoal(title: String, cat: String, hz: String, due: String, kpi: String = "", desc: String = "", pid: String = "") {
        val goal = GoalItem(
            id = UUID.randomUUID().toString(),
            t = title,
            cat = cat,
            hz = hz,
            due = due,
            st = "active",
            kpi = kpi,
            desc = desc,
            pid = pid
        )
        saveState(_state.value.copy(goal = listOf(goal) + _state.value.goal))
    }

    fun updateGoal(updatedGoal: GoalItem) {
        val list = _state.value.goal.map { if (it.id == updatedGoal.id) updatedGoal else it }
        saveState(_state.value.copy(goal = list))
    }

    fun addKeyResult(goalId: String, title: String) {
        val goal = _state.value.goal.find { it.id == goalId } ?: return
        val newKr = KeyResultItem(id = UUID.randomUUID().toString(), t = title, done = false)
        val updatedGoal = goal.copy(kr = goal.kr + newKr)
        updateGoal(updatedGoal)
    }

    fun toggleKeyResult(goalId: String, krId: String) {
        val goal = _state.value.goal.find { it.id == goalId } ?: return
        val updatedKr = goal.kr.map { if (it.id == krId) it.copy(done = !it.done) else it }
        val doneCount = updatedKr.count { it.done }
        val prog = if (updatedKr.isNotEmpty()) (doneCount.toDouble() / updatedKr.size * 100) else goal.prog
        val updatedGoal = goal.copy(kr = updatedKr, prog = prog)
        updateGoal(updatedGoal)
    }

    fun deleteKeyResult(goalId: String, krId: String) {
        val goal = _state.value.goal.find { it.id == goalId } ?: return
        val updatedKr = goal.kr.filterNot { it.id == krId }
        val doneCount = updatedKr.count { it.done }
        val prog = if (updatedKr.isNotEmpty()) (doneCount.toDouble() / updatedKr.size * 100) else goal.prog
        val updatedGoal = goal.copy(kr = updatedKr, prog = prog)
        updateGoal(updatedGoal)
    }

    fun deleteGoal(goalId: String) {
        val list = _state.value.goal.filterNot { it.id == goalId }
        val sessList = _state.value.sess.map { s ->
            s.copy(items = s.items.filterNot { it.kind == "goal" && it.ref == goalId })
        }
        saveState(_state.value.copy(goal = list, sess = sessList))
    }

    // --- Idea operations ---
    fun addIdea(title: String, type: String, prio: String, pid: String = "", desc: String = "") {
        val idea = IdeaItem(
            id = UUID.randomUUID().toString(),
            t = title,
            type = type,
            st = "new",
            prio = prio,
            pid = pid,
            desc = desc
        )
        saveState(_state.value.copy(idea = listOf(idea) + _state.value.idea))
    }

    fun updateIdea(updatedIdea: IdeaItem) {
        val list = _state.value.idea.map { if (it.id == updatedIdea.id) updatedIdea else it }
        saveState(_state.value.copy(idea = list))
    }

    fun addIdeaLog(ideaId: String, text: String, date: String = JalaliCalendar.today()) {
        val idea = _state.value.idea.find { it.id == ideaId } ?: return
        val logItem = ActionLogItem(id = UUID.randomUUID().toString(), d = date, txt = text)
        val updated = idea.copy(log = idea.log + logItem)
        updateIdea(updated)
    }

    fun deleteIdeaLog(ideaId: String, logId: String) {
        val idea = _state.value.idea.find { it.id == ideaId } ?: return
        val updated = idea.copy(log = idea.log.filterNot { it.id == logId })
        updateIdea(updated)
    }

    fun deleteIdea(ideaId: String) {
        val list = _state.value.idea.filterNot { it.id == ideaId }
        val sessList = _state.value.sess.map { s ->
            s.copy(items = s.items.filterNot { it.kind == "idea" && it.ref == ideaId })
        }
        saveState(_state.value.copy(idea = list, sess = sessList))
    }

    // --- Habit operations ---
    fun addHabit(name: String) {
        val habit = HabitItem(id = UUID.randomUUID().toString(), name = name)
        saveState(_state.value.copy(habit = _state.value.habit + habit))
    }

    fun toggleHabitDay(habitId: String, date: String) {
        val updated = _state.value.habit.map { h ->
            if (h.id == habitId) {
                val newLog = if (h.log.contains(date)) h.log - date else h.log + date
                h.copy(log = newLog)
            } else h
        }
        saveState(_state.value.copy(habit = updated))
    }

    fun toggleHabitToday(habitId: String) {
        toggleHabitDay(habitId, JalaliCalendar.today())
    }

    fun toggleHabitArchive(habitId: String) {
        val updated = _state.value.habit.map { h ->
            if (h.id == habitId) h.copy(arch = !h.arch) else h
        }
        saveState(_state.value.copy(habit = updated))
    }

    fun deleteHabit(habitId: String) {
        val list = _state.value.habit.filterNot { it.id == habitId }
        saveState(_state.value.copy(habit = list))
    }

    fun calculateStreak(habit: HabitItem): Int {
        val set = habit.log.toSet()
        val today = JalaliCalendar.today()
        var n = 0
        var i = if (set.contains(today)) 0 else 1
        while (i < 4000 && set.contains(JalaliCalendar.addDays(-i))) {
            n++
            i++
        }
        return n
    }

    // --- Workout operations ---
    fun addWorkout(type: String, min: Int, intVal: Int, date: String, note: String = "") {
        val w = WorkItem(
            id = UUID.randomUUID().toString(),
            d = date,
            type = type,
            min = min,
            intVal = intVal,
            note = note
        )
        saveState(_state.value.copy(work = listOf(w) + _state.value.work))
    }

    fun updateWorkout(w: WorkItem) {
        val list = _state.value.work.map { if (it.id == w.id) w else it }
        saveState(_state.value.copy(work = list))
    }

    fun deleteWorkout(workoutId: String) {
        val list = _state.value.work.filterNot { it.id == workoutId }
        saveState(_state.value.copy(work = list))
    }

    // --- Book and Reading Log operations ---
    fun addBook(title: String, author: String, pages: Int, note: String = "") {
        val b = BookItem(
            id = UUID.randomUUID().toString(),
            t = title,
            author = author,
            pages = pages,
            cur = 0,
            st = "want",
            note = note
        )
        saveState(_state.value.copy(book = listOf(b) + _state.value.book))
    }

    fun updateBook(b: BookItem) {
        val list = _state.value.book.map { if (it.id == b.id) b else it }
        saveState(_state.value.copy(book = list))
    }

    fun addReadingLog(bookId: String, pages: Int, min: Int, date: String) {
        val book = _state.value.book.find { it.id == bookId } ?: return
        val log = ReadingLogItem(id = UUID.randomUUID().toString(), bid = bookId, d = date, pages = pages, min = min)
        var newCur = book.cur + pages
        var newSt = book.st
        var doneAt = book.doneAt
        if (book.pages > 0 && newCur >= book.pages) {
            newCur = book.pages
            newSt = "done"
            doneAt = date
        } else if (newSt == "want" || newSt == "pause") {
            newSt = "reading"
        }
        val updatedBook = book.copy(cur = newCur, st = newSt, doneAt = doneAt)
        val bookList = _state.value.book.map { if (it.id == bookId) updatedBook else it }
        saveState(_state.value.copy(book = bookList, rlog = listOf(log) + _state.value.rlog))
    }

    fun deleteReadingLog(logId: String) {
        val list = _state.value.rlog.filterNot { it.id == logId }
        saveState(_state.value.copy(rlog = list))
    }

    fun deleteBook(bookId: String) {
        val bookList = _state.value.book.filterNot { it.id == bookId }
        val rlogList = _state.value.rlog.filterNot { it.bid == bookId }
        saveState(_state.value.copy(book = bookList, rlog = rlogList))
    }

    // --- Skill operations ---
    fun addSkill(name: String, target: Int = 100, plan: String = "", res: String = "") {
        val s = SkillItem(
            id = UUID.randomUUID().toString(),
            name = name,
            level = 0,
            target = target,
            plan = plan,
            res = res
        )
        saveState(_state.value.copy(skill = listOf(s) + _state.value.skill))
    }

    fun updateSkill(s: SkillItem) {
        val list = _state.value.skill.map { if (it.id == s.id) s else it }
        saveState(_state.value.copy(skill = list))
    }

    fun addSkillPractice(skillId: String, min: Int, note: String, date: String) {
        val p = SkillPracticeItem(
            id = UUID.randomUUID().toString(),
            sid = skillId,
            d = date,
            min = min,
            note = note
        )
        saveState(_state.value.copy(sprac = listOf(p) + _state.value.sprac))
    }

    fun deleteSkillPractice(practiceId: String) {
        val list = _state.value.sprac.filterNot { it.id == practiceId }
        saveState(_state.value.copy(sprac = list))
    }

    fun deleteSkill(skillId: String) {
        val skillList = _state.value.skill.filterNot { it.id == skillId }
        val spracList = _state.value.sprac.filterNot { it.sid == skillId }
        saveState(_state.value.copy(skill = skillList, sprac = spracList))
    }

    // --- Daily Journal operations ---
    fun saveJournal(date: String, mood: Int, gr: String, lesson: String, txt: String) {
        val existing = _state.value.jour.find { it.d == date }
        val item = existing?.copy(mood = mood, gr = gr, lesson = lesson, txt = txt)
            ?: DailyJournalItem(id = UUID.randomUUID().toString(), d = date, mood = mood, gr = gr, lesson = lesson, txt = txt)
        val list = _state.value.jour.filterNot { it.d == date } + item
        saveState(_state.value.copy(jour = list))
    }

    fun setTodayMood(mood: Int) {
        val today = JalaliCalendar.today()
        val existing = _state.value.jour.find { it.d == today }
        val nextMood = if (existing?.mood == mood) 0 else mood
        val item = existing?.copy(mood = nextMood)
            ?: DailyJournalItem(id = UUID.randomUUID().toString(), d = today, mood = nextMood)
        val list = _state.value.jour.filterNot { it.d == today } + item
        saveState(_state.value.copy(jour = list))
    }

    // --- Thinking Session operations ---
    fun addSession(title: String, kind: String, date: String, place: String = "", agenda: String = "") {
        val defaultAgenda = agenda.ifBlank {
            when (kind) {
                "think" -> "چه چیزی ذهنم را مشغول کرده است؟\nچه چیزی را نمی‌دانم و باید بدانم؟\nاگر مجبور باشم همین امروز یک قدم بردارم، آن قدم چیست؟"
                "week" -> "این هفته چه چیزهایی خوب پیش رفت؟\nچه چیزی کند شد و دلیلش چه بود؟\nسه اولویت هفتهٔ بعد کدامند؟"
                "month" -> "نسبت به اهدافم کجا هستم؟\nپول، سلامت، یادگیری و روابطم در این ماه چگونه بود؟\nماه بعد چه چیزی را شروع کنم، متوقف کنم یا ادامه بدهم؟"
                "decide" -> "مسئلهٔ پیش رو چیست؟\nگزینه‌ها کدامند و هر کدام چه هزینه و چه سودی دارد؟\nاگر یک سال بعد به این تصمیم نگاه کنم چه می‌گویم؟"
                "idea" -> "هدف از این ایده‌پردازی چیست؟\nچه محدودیت‌هایی دارم؟\nکوچک‌ترین نسخه‌ای که می‌شود امتحان کرد چیست؟"
                else -> ""
            }
        }
        val sess = ThinkingSessionItem(
            id = UUID.randomUUID().toString(),
            t = title,
            d = date,
            place = place,
            kind = kind,
            st = "plan",
            agenda = defaultAgenda
        )
        saveState(_state.value.copy(sess = listOf(sess) + _state.value.sess))
    }

    fun updateSession(s: ThinkingSessionItem) {
        val list = _state.value.sess.map { if (it.id == s.id) s else it }
        saveState(_state.value.copy(sess = list))
    }

    fun addSessionTopic(sessionId: String, kind: String, refId: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        if (sess.items.any { it.kind == kind && it.ref == refId }) return
        val topic = SessionTopicItem(id = UUID.randomUUID().toString(), kind = kind, ref = refId, verdict = "pending")
        val updated = sess.copy(items = sess.items + topic)
        // If it's an idea in 'new' state, shift it to 'review'
        var ideaList = _state.value.idea
        if (kind == "idea") {
            ideaList = ideaList.map { if (it.id == refId && it.st == "new") it.copy(st = "review") else it }
        }
        val sessList = _state.value.sess.map { if (it.id == sessionId) updated else it }
        saveState(_state.value.copy(sess = sessList, idea = ideaList))
    }

    fun updateTopicVerdict(sessionId: String, topicId: String, verdict: String, vnote: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        val topic = sess.items.find { it.id == topicId } ?: return
        val updatedTopic = topic.copy(verdict = verdict, vnote = vnote)
        val updatedSess = sess.copy(items = sess.items.map { if (it.id == topicId) updatedTopic else it })

        // Apply verdict to entity
        var goalList = _state.value.goal
        var ideaList = _state.value.idea
        if (topic.kind == "goal") {
            goalList = goalList.map { g ->
                if (g.id == topic.ref) {
                    when (verdict) {
                        "approved" -> if (g.st == "hold" || g.st == "dropped") g.copy(st = "active") else g
                        "rejected" -> if (g.st == "active") g.copy(st = "dropped") else g
                        else -> g
                    }
                } else g
            }
        } else if (topic.kind == "idea") {
            ideaList = ideaList.map { i ->
                if (i.id == topic.ref) {
                    when (verdict) {
                        "approved" -> i.copy(st = "approved")
                        "rejected" -> i.copy(st = "rejected")
                        "pending", "postponed" -> if (i.st == "approved" || i.st == "rejected") i.copy(st = "review") else i
                        else -> i
                    }
                } else i
            }
        }
        val sessList = _state.value.sess.map { if (it.id == sessionId) updatedSess else it }
        saveState(_state.value.copy(sess = sessList, goal = goalList, idea = ideaList))
    }

    fun removeSessionTopic(sessionId: String, topicId: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        val updated = sess.copy(items = sess.items.filterNot { it.id == topicId })
        val sessList = _state.value.sess.map { if (it.id == sessionId) updated else it }
        saveState(_state.value.copy(sess = sessList))
    }

    fun addSessionResolution(sessionId: String, text: String, due: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        val res = SessionResolutionItem(
            id = UUID.randomUUID().toString(),
            t = text,
            due = due,
            st = "todo"
        )
        val updated = sess.copy(res = sess.res + res)
        val sessList = _state.value.sess.map { if (it.id == sessionId) updated else it }
        saveState(_state.value.copy(sess = sessList))
    }

    fun convertResolutionToTask(sessionId: String, resId: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        val res = sess.res.find { it.id == resId } ?: return
        if (res.tid.isNotBlank()) return

        val taskId = UUID.randomUUID().toString()
        val newTask = TaskItem(
            id = taskId,
            t = res.t,
            area = "personal",
            due = res.due,
            prio = "mid",
            st = res.st,
            desc = "مصوبهٔ جلسه: ${sess.t}"
        )
        val updatedRes = res.copy(tid = taskId)
        val updatedSess = sess.copy(res = sess.res.map { if (it.id == resId) updatedRes else it })
        val sessList = _state.value.sess.map { if (it.id == sessionId) updatedSess else it }
        val taskList = listOf(newTask) + _state.value.task
        saveState(_state.value.copy(sess = sessList, task = taskList))
    }

    fun updateResolutionStatus(sessionId: String, resId: String, status: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        val updated = sess.copy(res = sess.res.map { if (it.id == resId) it.copy(st = status) else it })
        val sessList = _state.value.sess.map { if (it.id == sessionId) updated else it }
        saveState(_state.value.copy(sess = sessList))
    }

    fun deleteSessionResolution(sessionId: String, resId: String) {
        val sess = _state.value.sess.find { it.id == sessionId } ?: return
        val updated = sess.copy(res = sess.res.filterNot { it.id == resId })
        val sessList = _state.value.sess.map { if (it.id == sessionId) updated else it }
        saveState(_state.value.copy(sess = sessList))
    }

    fun deleteSession(sessionId: String) {
        val sessList = _state.value.sess.filterNot { it.id == sessionId }
        saveState(_state.value.copy(sess = sessList))
    }

    private fun createDefaultSeedData(): NotebookState {
        val today = JalaliCalendar.today()
        val p1 = ProjectItem(
            id = "p-branding",
            name = "توسعه برند شخصی و وب‌سایت",
            status = "doing",
            prio = "high",
            start = JalaliCalendar.addDays(-15),
            due = JalaliCalendar.addDays(30),
            budget = 8500000.0,
            desc = "طراحی پورتفولیو، هویت بصری و انتشار مقالات تخصصی"
        )
        val p2 = ProjectItem(
            id = "p-office",
            name = "تجهیز اتاق کار خانگی",
            status = "doing",
            prio = "mid",
            start = JalaliCalendar.addDays(-20),
            due = JalaliCalendar.addDays(10),
            budget = 12000000.0,
            desc = "تهیه صندلی ارگونومیک، میز تنظیم‌شونده و نورپردازی مناسب کار"
        )

        val tasks = listOf(
            TaskItem(id = "t1", t = "بررسی گزارش مالی ماهانه و بستن حساب‌ها", area = "work", prio = "high", due = today, st = "todo"),
            TaskItem(id = "t2", t = "نیم ساعت پیاده‌روی عصرگاهی در پارک", area = "personal", prio = "mid", due = today, st = "todo"),
            TaskItem(id = "t3", t = "نهایی‌سازی نقشه راه پروژه توسعه برند", area = "work", pid = "p-branding", prio = "high", due = JalaliCalendar.addDays(2), st = "doing"),
            TaskItem(id = "t4", t = "مطالعه ۲۰ صفحه از کتاب عادت‌های اتمی", area = "study", prio = "mid", due = today, st = "done", doneAt = today)
        )

        val txs = listOf(
            TxItem(id = "tx1", type = "in", t = "دریافت پیش‌پرداخت پروژه مشاوره", cat = "پروژه", amt = 18000000.0, d = today, pid = "p-branding"),
            TxItem(id = "tx2", type = "out", t = "خرید کتاب‌های تخصصی و دفتر یادداشت", cat = "آموزش", amt = 850000.0, d = today),
            TxItem(id = "tx3", type = "out", t = "حق عضویت باشگاه ورزشی ماهانه", cat = "سلامت", amt = 1200000.0, d = JalaliCalendar.addDays(-3)),
            TxItem(id = "tx4", type = "out", t = "خرید پایه لپ‌تاپ آلومینیومی", cat = "خرید", amt = 650000.0, d = JalaliCalendar.addDays(-5), pid = "p-office")
        )

        val buys = listOf(
            BuyItem(id = "b1", t = "صندلی ارگونومیک اروند", price = 6800000.0, kind = "need", prio = "high", st = "plan", note = "برای اتاق کار خانگی"),
            BuyItem(id = "b2", t = "هدفون نویزکنسلینگ سونی", price = 11500000.0, kind = "want", prio = "mid", st = "want", note = "برای تمرکز در کار و سفر"),
            BuyItem(id = "b3", t = "پایه لپ‌تاپ آلومینیومی", price = 650000.0, kind = "need", prio = "high", st = "bought", d = JalaliCalendar.addDays(-5), txid = "tx4")
        )

        val goals = listOf(
            GoalItem(
                id = "g1",
                t = "مطالعه ۱۲ جلد کتاب تا پایان سال",
                cat = "learning",
                hz = "short",
                due = JalaliCalendar.addDays(120),
                st = "active",
                prog = 33.0,
                kpi = "خواندن ماهانه یک کتاب و خلاصه‌نویسی آن",
                kr = listOf(
                    KeyResultItem(id = "kr1", t = "مطالعه کتاب عادت‌های اتمی", done = true),
                    KeyResultItem(id = "kr2", t = "مطالعه کتاب کار عمیق", done = true),
                    KeyResultItem(id = "kr3", t = "مطالعه کتاب مینیمالیسم دیجیتال", done = false),
                    KeyResultItem(id = "kr4", t = "مطالعه کتاب تفکر سریع و کند", done = false)
                )
            ),
            GoalItem(
                id = "g2",
                t = "رسیدن به وزن ایده‌آل و تناسب اندام پایدار",
                cat = "health",
                hz = "mid",
                due = JalaliCalendar.addDays(90),
                st = "active",
                prog = 40.0,
                kpi = "۳ روز در هفته ورزش و اصلاح تغذیه"
            )
        )

        val ideas = listOf(
            IdeaItem(
                id = "i1",
                t = "راه‌اندازی خبرنامه هفتگی رشد و بهره‌وری فردی",
                type = "plan",
                st = "approved",
                prio = "high",
                pid = "p-branding",
                desc = "یک ایمیل ۵ دقیقه‌ای شامل یک درس، یک ابزار و یک سوال برای تفکر عمیق",
                prog = 20.0,
                log = listOf(
                    ActionLogItem(id = "l1", d = JalaliCalendar.addDays(-7), txt = "تعیین پلتفرم ارسال ایمیل و طراحی فرم اشتراک")
                )
            ),
            IdeaItem(
                id = "i2",
                t = "توسعه دوره آموزشی مدیریت زمان برای فریلنسرها",
                type = "idea",
                st = "new",
                prio = "mid",
                desc = "سرفصل‌ها: تمرکز عمیق، بلوک‌بندی زمان و مدیریت انرژی روزانه"
            )
        )

        val habits = listOf(
            HabitItem(
                id = "h1",
                name = "مطالعه روزانه (حداقل ۲۰ دقیقه)",
                log = listOf(today, JalaliCalendar.addDays(-1), JalaliCalendar.addDays(-2), JalaliCalendar.addDays(-3), JalaliCalendar.addDays(-5))
            ),
            HabitItem(
                id = "h2",
                name = "ورزش یا پیاده‌روی صبحگاهی",
                log = listOf(today, JalaliCalendar.addDays(-1), JalaliCalendar.addDays(-2))
            ),
            HabitItem(
                id = "h3",
                name = "نوشتن دفتر سپاسگزاری شبانه",
                log = listOf(JalaliCalendar.addDays(-1), JalaliCalendar.addDays(-2), JalaliCalendar.addDays(-3), JalaliCalendar.addDays(-4))
            )
        )

        val workouts = listOf(
            WorkItem(id = "w1", d = today, type = "پیاده‌روی", min = 35, intVal = 2, note = "مسیر پارک ملت"),
            WorkItem(id = "w2", d = JalaliCalendar.addDays(-1), type = "باشگاه و وزنه", min = 55, intVal = 3, note = "تمرین بالاتنه"),
            WorkItem(id = "w3", d = JalaliCalendar.addDays(-3), type = "دویدن", min = 25, intVal = 3, note = "هوازی اینتروال")
        )

        val books = listOf(
            BookItem(id = "bk1", t = "عادت‌های اتمی", author = "جیمز کلیر", pages = 320, cur = 240, st = "reading", rate = 5, note = "تغییرات کوچک نتایج چشمگیری ایجاد می‌کنند."),
            BookItem(id = "bk2", t = "کار عمیق", author = "کال نیوپورت", pages = 280, cur = 280, st = "done", rate = 5, note = "تمرکز بدون حواس‌پرتی کمیاب‌ترین مهارت قرن ۲۱ است.", doneAt = JalaliCalendar.addDays(-10)),
            BookItem(id = "bk3", t = "شجاعت در ناشناخته‌ها", author = "برنه براون", pages = 310, cur = 0, st = "want", rate = 0)
        )

        val skills = listOf(
            SkillItem(id = "sk1", name = "طراحی سیستم و معماری نرم‌افزار", level = 65, target = 90, plan = "مطالعه کتب مرجع، دوره‌های تخصصی و پیاده‌سازی نمونه پروژه‌ها", res = "مستندات Martin Fowler، کتب طراحی سیستم"),
            SkillItem(id = "sk2", name = "زبان انگلیسی (مکالمه و نگارش)", level = 70, target = 85, plan = "گوش دادن به پادکست روزانه و نوشتن یادداشت انگلیسی", res = "پادکست BBC 6 Minute English")
        )

        val journal = listOf(
            DailyJournalItem(
                id = "j1",
                d = today,
                mood = 4,
                gr = "سلامتی خودم و خانواده، هوای مطبوع بارانی، پیشرفت در کارها",
                lesson = "تمرکز روی یک کار در هر زمان کیفیت را دوبرابر و استرس را نصف می‌کند.",
                txt = "امروز حس خوبی به کارهای پیش‌رو داشتم. جلسه صبح موثر بود."
            )
        )

        val sessions = listOf(
            ThinkingSessionItem(
                id = "s1",
                t = "مرور هفتگی و تنظیم اولویت‌های کاری",
                d = JalaliCalendar.addDays(-2),
                place = "کافه مطالعه",
                kind = "week",
                st = "held",
                agenda = "این هفته چه چیزهایی خوب پیش رفت؟\nچه چیزی کند شد و دلیلش چه بود؟\nسه اولویت هفتهٔ بعد کدامند؟",
                notes = "تمرکز روی پروژه برند شخصی خیلی بهتر شد. باید هزینه‌های جاری را کنترل‌تر کنم.",
                items = listOf(
                    SessionTopicItem(id = "st1", kind = "proj", ref = "p-branding", verdict = "approved", vnote = "فاز اول تایید شد"),
                    SessionTopicItem(id = "st2", kind = "goal", ref = "g1", verdict = "approved", vnote = "سرعت مطالعه مناسب است")
                ),
                res = listOf(
                    SessionResolutionItem(id = "sr1", t = "بررسی گزارش مالی ماهانه و بستن حساب‌ها", due = today, st = "todo", tid = "t1"),
                    SessionResolutionItem(id = "sr2", t = "ثبت‌نام در کارگاه طراحی رابط کاربری", due = JalaliCalendar.addDays(5), st = "todo")
                )
            )
        )

        val budgets = mapOf(
            "خوراک" to 5000000.0,
            "آموزش" to 2000000.0,
            "سلامت" to 1500000.0,
            "خرید" to 3000000.0,
            "تفریح" to 1500000.0
        )

        return NotebookState(
            set = NotebookSettings(budgets = budgets),
            proj = listOf(p1, p2),
            task = tasks,
            tx = txs,
            buy = buys,
            goal = goals,
            idea = ideas,
            book = books,
            work = workouts,
            skill = skills,
            habit = habits,
            jour = journal,
            sess = sessions
        )
    }
}
