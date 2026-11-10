package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NotebookRepository
import com.example.data.model.*
import com.example.util.JalaliCalendar
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppSection(val title: String) {
    TODAY("امروز"),
    SEARCH("جستجو"),
    TASKS("تسک‌ها"),
    PROJECTS("پروژه‌ها"),
    FINANCE("مالی"),
    BUY("خریدها"),
    GOALS("اهداف"),
    IDEAS("ایده‌ها"),
    GROW("رشد فردی"),
    SESSIONS("جلسات تفکر")
}

enum class GrowSubTab(val title: String) {
    HABIT("عادت‌ها"),
    WORK("ورزش"),
    READ("مطالعه"),
    SKILL("مهارت‌ها"),
    JOURNAL("دفتر روزانه")
}

data class SearchResultItem(
    val section: AppSection,
    val growTab: GrowSubTab? = null,
    val label: String,
    val title: String,
    val subtitle: String,
    val id: String
)

class NotebookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NotebookRepository(application)
    val state: StateFlow<NotebookState> = repository.state

    private val _currentSection = MutableStateFlow(AppSection.TODAY)
    val currentSection: StateFlow<AppSection> = _currentSection.asStateFlow()

    private val _growSubTab = MutableStateFlow(GrowSubTab.HABIT)
    val growSubTab: StateFlow<GrowSubTab> = _growSubTab.asStateFlow()

    // Finance navigation
    private val _financeMonth = MutableStateFlow(JalaliCalendar.currentYearMonth())
    val financeMonth: StateFlow<Pair<Int, Int>> = _financeMonth.asStateFlow()

    private val _financeShowAll = MutableStateFlow(false)
    val financeShowAll: StateFlow<Boolean> = _financeShowAll.asStateFlow()

    // Filters
    val taskStatusFilter = MutableStateFlow("open") // open, done, all
    val taskAreaFilter = MutableStateFlow("")
    val taskProjectFilter = MutableStateFlow("")
    val taskQuery = MutableStateFlow("")

    val projectStatusFilter = MutableStateFlow("")
    val projectQuery = MutableStateFlow("")

    val financeTypeFilter = MutableStateFlow("") // "", out, in
    val financeQuery = MutableStateFlow("")

    val buyStatusFilter = MutableStateFlow("open") // open, bought, drop, all

    val goalStatusFilter = MutableStateFlow("")
    val goalCatFilter = MutableStateFlow("")
    val goalQuery = MutableStateFlow("")

    val ideaStatusFilter = MutableStateFlow("")
    val ideaTypeFilter = MutableStateFlow("")
    val ideaQuery = MutableStateFlow("")

    val globalSearchQuery = MutableStateFlow("")

    val journalSelectedDate = MutableStateFlow(JalaliCalendar.today())

    // Dark mode: null = system, true = dark, false = light
    val isDarkMode = MutableStateFlow<Boolean?>(null)

    val isBackupDialogOpen = MutableStateFlow(false)

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    fun navigateTo(section: AppSection, growTab: GrowSubTab? = null) {
        _currentSection.value = section
        if (growTab != null) {
            _growSubTab.value = growTab
        }
    }

    fun setGrowSubTab(tab: GrowSubTab) {
        _growSubTab.value = tab
    }

    fun toggleDarkMode() {
        val current = isDarkMode.value
        isDarkMode.value = when (current) {
            null -> true
            true -> false
            false -> null
        }
    }

    fun showToast(message: String) {
        viewModelScope.launch {
            _snackbarMessage.emit(message)
        }
    }

    // --- Task actions ---
    fun addTask(title: String, area: String, pid: String, due: String, prio: String, desc: String = "") {
        repository.addTask(title, area, pid, due, prio, desc)
        showToast("تسک افزوده شد")
    }

    fun addQuickTaskToday(title: String, area: String) {
        repository.addQuickTaskToday(title, area)
        showToast("تسک به امروز افزوده شد")
    }

    fun toggleTask(taskId: String) {
        repository.toggleTask(taskId)
    }

    fun updateTask(task: TaskItem) {
        repository.updateTask(task)
    }

    fun deleteTask(taskId: String) {
        repository.deleteTask(taskId)
        showToast("تسک حذف شد")
    }

    // --- Project actions ---
    fun addProject(name: String, budget: Double, due: String, link: String, desc: String) {
        repository.addProject(name, budget, due, link, desc)
        showToast("پروژه ایجاد شد")
    }

    fun updateProject(proj: ProjectItem) {
        repository.updateProject(proj)
    }

    fun deleteProject(projId: String) {
        repository.deleteProject(projId)
        showToast("پروژه حذف شد")
    }

    // --- Finance actions ---
    fun prevFinanceMonth() {
        _financeMonth.value = JalaliCalendar.addMonths(_financeMonth.value, -1)
    }

    fun nextFinanceMonth() {
        _financeMonth.value = JalaliCalendar.addMonths(_financeMonth.value, 1)
    }

    fun toggleFinanceShowAll() {
        _financeShowAll.value = !_financeShowAll.value
    }

    fun addTransaction(type: String, title: String, cat: String, amt: Double, date: String, pid: String = "", bid: String = "") {
        repository.addTransaction(type, title, cat, amt, date, pid, bid)
        showToast("تراکنش ثبت شد")
    }

    fun updateTransaction(tx: TxItem) {
        repository.updateTransaction(tx)
    }

    fun deleteTransaction(txId: String) {
        repository.deleteTransaction(txId)
        showToast("تراکنش حذف شد")
    }

    fun setBudgetLimit(cat: String, amount: Double) {
        repository.setBudgetLimit(cat, amount)
        showToast("سقف بودجه به‌روز شد")
    }

    // --- Shopping actions ---
    fun addBuyItem(title: String, price: Double, kind: String, prio: String, link: String = "", note: String = "") {
        repository.addBuyItem(title, price, kind, prio, link, note)
        showToast("به فهرست خرید اضافه شد")
    }

    fun markBuyItemBought(buyId: String) {
        val createdTx = repository.markBuyItemBought(buyId)
        if (createdTx) {
            showToast("خریده شد و به هزینه‌های مالی اضافه شد")
        } else {
            showToast("خریده شد")
        }
    }

    fun updateBuyItem(item: BuyItem) {
        repository.updateBuyItem(item)
    }

    fun deleteBuyItem(buyId: String) {
        repository.deleteBuyItem(buyId)
        showToast("مورد از فهرست خرید حذف شد")
    }

    // --- Goal actions ---
    fun addGoal(title: String, cat: String, hz: String, due: String, kpi: String, desc: String, pid: String) {
        repository.addGoal(title, cat, hz, due, kpi, desc, pid)
        showToast("هدف ثبت شد")
    }

    fun updateGoal(goal: GoalItem) {
        repository.updateGoal(goal)
    }

    fun addKeyResult(goalId: String, title: String) {
        repository.addKeyResult(goalId, title)
    }

    fun toggleKeyResult(goalId: String, krId: String) {
        repository.toggleKeyResult(goalId, krId)
    }

    fun deleteKeyResult(goalId: String, krId: String) {
        repository.deleteKeyResult(goalId, krId)
    }

    fun deleteGoal(goalId: String) {
        repository.deleteGoal(goalId)
        showToast("هدف حذف شد")
    }

    // --- Idea actions ---
    fun addIdea(title: String, type: String, prio: String, pid: String, desc: String) {
        repository.addIdea(title, type, prio, pid, desc)
        showToast("ایده ثبت شد")
    }

    fun updateIdea(idea: IdeaItem) {
        repository.updateIdea(idea)
    }

    fun addIdeaLog(ideaId: String, text: String, date: String) {
        repository.addIdeaLog(ideaId, text, date)
        showToast("اقدام پیگیری ثبت شد")
    }

    fun deleteIdeaLog(ideaId: String, logId: String) {
        repository.deleteIdeaLog(ideaId, logId)
    }

    fun deleteIdea(ideaId: String) {
        repository.deleteIdea(ideaId)
        showToast("ایده حذف شد")
    }

    // --- Habit actions ---
    fun addHabit(name: String) {
        repository.addHabit(name)
        showToast("عادت افزوده شد")
    }

    fun toggleHabitToday(habitId: String) {
        repository.toggleHabitToday(habitId)
    }

    fun toggleHabitDay(habitId: String, date: String) {
        repository.toggleHabitDay(habitId, date)
    }

    fun toggleHabitArchive(habitId: String) {
        repository.toggleHabitArchive(habitId)
    }

    fun deleteHabit(habitId: String) {
        repository.deleteHabit(habitId)
        showToast("عادت حذف شد")
    }

    fun getHabitStreak(habit: HabitItem): Int {
        return repository.calculateStreak(habit)
    }

    // --- Workout actions ---
    fun addWorkout(type: String, min: Int, intVal: Int, date: String, note: String) {
        repository.addWorkout(type, min, intVal, date, note)
        showToast("تمرین ورزشی ثبت شد")
    }

    fun updateWorkout(w: WorkItem) {
        repository.updateWorkout(w)
    }

    fun deleteWorkout(workoutId: String) {
        repository.deleteWorkout(workoutId)
        showToast("تمرین حذف شد")
    }

    // --- Book actions ---
    fun addBook(title: String, author: String, pages: Int, note: String) {
        repository.addBook(title, author, pages, note)
        showToast("کتاب افزوده شد")
    }

    fun updateBook(b: BookItem) {
        repository.updateBook(b)
    }

    fun addReadingLog(bookId: String, pages: Int, min: Int, date: String) {
        repository.addReadingLog(bookId, pages, min, date)
        showToast("مطالعه ثبت شد")
    }

    fun deleteReadingLog(logId: String) {
        repository.deleteReadingLog(logId)
    }

    fun deleteBook(bookId: String) {
        repository.deleteBook(bookId)
        showToast("کتاب حذف شد")
    }

    // --- Skill actions ---
    fun addSkill(name: String, target: Int, plan: String, res: String) {
        repository.addSkill(name, target, plan, res)
        showToast("مهارت افزوده شد")
    }

    fun updateSkill(s: SkillItem) {
        repository.updateSkill(s)
    }

    fun addSkillPractice(skillId: String, min: Int, note: String, date: String) {
        repository.addSkillPractice(skillId, min, note, date)
        showToast("تمرین مهارت ثبت شد")
    }

    fun deleteSkillPractice(practiceId: String) {
        repository.deleteSkillPractice(practiceId)
    }

    fun deleteSkill(skillId: String) {
        repository.deleteSkill(skillId)
        showToast("مهارت حذف شد")
    }

    // --- Journal actions ---
    fun saveJournal(date: String, mood: Int, gr: String, lesson: String, txt: String) {
        repository.saveJournal(date, mood, gr, lesson, txt)
        showToast("یادداشت روزانه ذخیره شد")
    }

    fun setTodayMood(mood: Int) {
        repository.setTodayMood(mood)
    }

    // --- Thinking Session actions ---
    fun addSession(title: String, kind: String, date: String, place: String, agenda: String) {
        repository.addSession(title, kind, date, place, agenda)
        showToast("جلسه ایجاد شد")
    }

    fun updateSession(s: ThinkingSessionItem) {
        repository.updateSession(s)
    }

    fun addSessionTopic(sessionId: String, kind: String, refId: String) {
        repository.addSessionTopic(sessionId, kind, refId)
    }

    fun updateTopicVerdict(sessionId: String, topicId: String, verdict: String, vnote: String) {
        repository.updateTopicVerdict(sessionId, topicId, verdict, vnote)
    }

    fun removeSessionTopic(sessionId: String, topicId: String) {
        repository.removeSessionTopic(sessionId, topicId)
    }

    fun addSessionResolution(sessionId: String, text: String, due: String) {
        repository.addSessionResolution(sessionId, text, due)
        showToast("مصوبه افزوده شد")
    }

    fun convertResolutionToTask(sessionId: String, resId: String) {
        repository.convertResolutionToTask(sessionId, resId)
        showToast("به تسک‌ها تبدیل شد")
    }

    fun updateResolutionStatus(sessionId: String, resId: String, status: String) {
        repository.updateResolutionStatus(sessionId, resId, status)
    }

    fun deleteSessionResolution(sessionId: String, resId: String) {
        repository.deleteSessionResolution(sessionId, resId)
    }

    fun deleteSession(sessionId: String) {
        repository.deleteSession(sessionId)
        showToast("جلسه حذف شد")
    }

    // --- Backup & Restore ---
    suspend fun exportJson(): String {
        return repository.exportJson()
    }

    suspend fun importJson(json: String): Boolean {
        val success = repository.importJson(json)
        if (success) {
            showToast("داده‌ها با موفقیت بازیابی شد")
        } else {
            showToast("خطا در بازیابی پشتیبان: ساختار JSON نامعتبر است")
        }
        return success
    }

    // --- Global Search ---
    fun searchAll(q: String): List<SearchResultItem> {
        val query = q.trim().lowercase()
        if (query.isBlank()) return emptyList()

        val results = mutableListOf<SearchResultItem>()
        val st = state.value

        // Tasks
        st.task.forEach { t ->
            if (t.t.lowercase().contains(query) || t.desc.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.TASKS, null, "تسک", t.t, t.due, t.id))
            }
        }
        // Projects
        st.proj.forEach { p ->
            if (p.name.lowercase().contains(query) || p.desc.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.PROJECTS, null, "پروژه", p.name, p.due, p.id))
            }
        }
        // Transactions
        st.tx.forEach { x ->
            if (x.t.lowercase().contains(query) || x.cat.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.FINANCE, null, if (x.type == "in") "درآمد" else "هزینه", x.t, "${x.d} · ${JalaliCalendar.formatToman(x.amt)}", x.id))
            }
        }
        // Buy items
        st.buy.forEach { b ->
            if (b.t.lowercase().contains(query) || b.note.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.BUY, null, "خرید", b.t, JalaliCalendar.formatToman(b.price), b.id))
            }
        }
        // Goals
        st.goal.forEach { g ->
            if (g.t.lowercase().contains(query) || g.desc.lowercase().contains(query) || g.kpi.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.GOALS, null, "هدف", g.t, g.due, g.id))
            }
        }
        // Ideas
        st.idea.forEach { i ->
            if (i.t.lowercase().contains(query) || i.desc.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.IDEAS, null, "ایده", i.t, "", i.id))
            }
        }
        // Books
        st.book.forEach { bk ->
            if (bk.t.lowercase().contains(query) || bk.author.lowercase().contains(query) || bk.note.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.GROW, GrowSubTab.READ, "کتاب", bk.t, bk.author, bk.id))
            }
        }
        // Workouts
        st.work.forEach { w ->
            if (w.type.lowercase().contains(query) || w.note.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.GROW, GrowSubTab.WORK, "ورزش", w.type, "${w.d} · ${w.min} دقیقه", w.id))
            }
        }
        // Skills
        st.skill.forEach { s ->
            if (s.name.lowercase().contains(query) || s.plan.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.GROW, GrowSubTab.SKILL, "مهارت", s.name, "سطح ${s.level}٪", s.id))
            }
        }
        // Habits
        st.habit.forEach { h ->
            if (h.name.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.GROW, GrowSubTab.HABIT, "عادت", h.name, "", h.id))
            }
        }
        // Journal
        st.jour.forEach { j ->
            if (j.txt.lowercase().contains(query) || j.gr.lowercase().contains(query) || j.lesson.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.GROW, GrowSubTab.JOURNAL, "دفتر روزانه", j.d, j.lesson.ifBlank { j.gr }.take(60), j.id))
            }
        }
        // Thinking Sessions
        st.sess.forEach { s ->
            if (s.t.lowercase().contains(query) || s.agenda.lowercase().contains(query) || s.notes.lowercase().contains(query)) {
                results.add(SearchResultItem(AppSection.SESSIONS, null, "جلسه تفکر", s.t, s.d, s.id))
            }
            s.res.forEach { r ->
                if (r.t.lowercase().contains(query)) {
                    results.add(SearchResultItem(AppSection.SESSIONS, null, "مصوبه جلسه", r.t, "جلسه: ${s.t}", r.id))
                }
            }
        }

        return results.take(100)
    }
}
