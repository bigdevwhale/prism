package app.prism.data

import android.content.Context
import app.prism.content.Library
import app.prism.domain.Gender
import app.prism.domain.gendered
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/** Made-up roles, scenes and practices for screenshots (`-Pdemo`). */
object DemoSeed {
    suspend fun seedIfEmpty(context: Context, dao: PrismDao, settings: SettingsStore) {
        if (dao.spheres().isNotEmpty()) return
        val locale = context.resources.configuration.locales[0]
        val ru = locale.language == "ru"
        fun t(en: String, r: String) = if (ru) r else en
        val lib = Library.of(locale)
        val fix: (String) -> String = { gendered(it, Gender.FEMALE) }
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val day = today.toEpochDay()
        fun at(d: Long, minutes: Int): Long = LocalDate.ofEpochDay(d).atStartOfDay(zone).plusMinutes(minutes.toLong()).toInstant().toEpochMilli()
        val now = System.currentTimeMillis()

        settings.update { it.copy(name = t("Anna", "Аня"), gender = Gender.FEMALE, onboarded = true) }
        dao.setCore(listOf(t("Warmth", "Тепло"), t("Honesty", "Честность"), t("Calm", "Спокойствие"), t("Growth", "Рост")))
        dao.setupSpheres(listOf("home", "partner", "kids", "work", "lead", "friends", "parents", "self", "conflict"))
        val dogSphere = dao.upsertSphere(Sphere(name = t("Dog", "Собака"), icon = "PET", hue = 6, sort = 9))

        val keys = listOf(
            "home.harbor", "partner.ally", "kids.playmate", "kids.base", "work.craftsman",
            "lead.servant", "friends.own", "parents.grownup", "self.friend", "conflict.mountain",
        )
        val ids = keys.associateWith { k -> dao.adopt(lib.role(k)!!, now, fix) }
        fun id(k: String) = ids.getValue(k)
        // A role of her own, in her own sphere.
        val pack = dao.upsertRole(
            Role(
                sphereId = dogSphere,
                name = t("Pack leader", "Вожак стаи"),
                tagline = t("Calm and clear, so the dog feels safe.", "Спокойная и понятная — рядом со мной собаке ничего не страшно."),
                qualities = t("calm\nclear\npatient", "спокойная\nпонятная\nтерпеливая"),
                voice = t("Few words, the same every time, never shouting.", "Мало слов, всегда одни и те же, без крика."),
                does = t("Walk without the phone in my hand\nPraise the moment it gets it right\nKeep the same rules every day",
                    "Гуляю без телефона в руке\nХвалю в ту же секунду, когда получилось\nДержу одни и те же правила каждый день"),
                never = t("Never punish out of my own bad mood\nNever rush the morning walk", "Не наказываю из-за своего плохого настроения\nНе тороплю утреннюю прогулку"),
                body = t("Loose shoulders, slow steps, a soft hand on the lead.", "Плечи свободны, шаг медленный, поводок в мягкой руке."),
                motto = t("Steady hand, kind heart.", "Твёрдая рука, доброе сердце."),
                createdAt = now,
            )
        )

        val prosecutor = dao.adoptShadow(lib.shadow("shadow.prosecutor")!!, now, fix, dao.spheres().first { it.presetKey == "conflict" }.id)
        val workaholic = dao.adoptShadow(lib.shadow("shadow.workaholic")!!, now, fix, dao.spheres().first { it.presetKey == "work" }.id)
        val scroller = dao.adoptShadow(lib.shadow("shadow.scroller")!!, now, fix)
        listOf(prosecutor to id("conflict.mountain"), workaholic to id("home.harbor"), scroller to id("kids.playmate")).forEach { (s, a) ->
            dao.role(s)?.let { dao.upsertRole(it.copy(antidoteId = a)) }
        }

        // Five weeks of evenings: scores drift up, energy depends on the role, shadows drop by now and then.
        val r = Random(42)
        val energyBias = mapOf(
            id("home.harbor") to 1, id("kids.playmate") to 2, id("partner.ally") to 1, id("friends.own") to 2,
            id("work.craftsman") to 0, id("lead.servant") to -1, id("parents.grownup") to -1, id("conflict.mountain") to -1,
            id("self.friend") to 1, id("kids.base") to 1, pack to 2,
        )
        val shadowFor = mapOf(
            id("work.craftsman") to workaholic, id("lead.servant") to workaholic, id("home.harbor") to prosecutor,
            id("partner.ally") to prosecutor, id("kids.playmate") to scroller, id("kids.base") to scroller, id("conflict.mountain") to prosecutor,
        )
        val daily = listOf(id("home.harbor"), id("work.craftsman"), id("kids.playmate"))
        val sometimes = listOf(id("partner.ally"), id("lead.servant"), id("friends.own"), id("parents.grownup"), id("self.friend"), id("kids.base"), id("conflict.mountain"), pack)
        for (back in 34 downTo 1) {
            val d = day - back
            if (r.nextFloat() < 0.14f) continue
            val trend = 45 + (34 - back) * 40 / 34
            val played = (daily.shuffled(r).take(2 + r.nextInt(2)) + sometimes.shuffled(r).take(r.nextInt(3))).distinct().take(4)
            played.forEachIndexed { i, role ->
                val shadow = if (r.nextFloat() < 0.13f - (34 - back) * 0.002f) shadowFor[role] ?: 0L else 0L
                val score = (trend + r.nextInt(-12, 13) - if (shadow != 0L) 20 else 0).coerceIn(5, 100)
                val energy = ((energyBias[role] ?: 0) + r.nextInt(-1, 2)).coerceIn(-2, 2)
                dao.insertScene(Scene(day = d, at = at(d, 21 * 60 + 30 + i), roleId = role, kind = Scene.REVIEW, score = score, energy = energy, shadowId = shadow))
            }
            if (d.toInt() % 7 != 0 && LocalDate.ofEpochDay(d).dayOfWeek.value <= 5) {
                dao.insertScene(Scene(day = d, at = at(d, 18 * 60 + 30), roleId = id("home.harbor"), kind = Scene.THRESHOLD))
            }
            if (r.nextFloat() < 0.07f) dao.insertScene(Scene(day = d, at = at(d, 22 * 60 + 40), roleId = scroller, kind = Scene.SHADOW))
            dao.upsertDay(DayPlan(day = d, focusRoleId = played.first(), reviewed = true))
        }
        // Today: a role chosen in the morning and a threshold crossed, the review still ahead.
        dao.upsertDay(DayPlan(day = day, focusRoleId = id("home.harbor"), intention = t("Leave work at the door and really listen at dinner", "Оставить работу за порогом и по-настоящему слушать за ужином")))
        dao.insertScene(Scene(day = day, at = at(day, 9 * 60), roleId = id("work.craftsman"), kind = Scene.THRESHOLD))
        dao.insertScene(Scene(day = day, at = at(day, 13 * 60), roleId = id("conflict.mountain"), kind = Scene.QUICK, score = 78, energy = 0, note = t("Didn't snap back at the client", "Не огрызнулась на клиента")))

        // Acts: one running, one finished.
        val running = dao.upsertAct(Act(roleId = id("kids.playmate"), startDay = day - 8, length = 21))
        listOf(8, 7, 6, 4, 3, 1).forEach { dao.insertActDay(ActDay(running, day - it, at(day - it, 20 * 60))) }
        val finished = dao.upsertAct(Act(roleId = id("home.harbor"), startDay = day - 30, length = 14))
        (0 until 14).filter { it != 3 && it != 9 }.forEach { dao.insertActDay(ActDay(finished, day - 30 + it, at(day - 30 + it, 20 * 60))) }

        dao.upsertRehearsal(
            Rehearsal(
                roleId = id("work.craftsman"),
                title = t("Asking for a raise", "Разговор о повышении"),
                day = day,
                goal = t("Leave with a date for the decision", "Уйти с конкретной датой решения"),
                say = t("I've led the launch for half a year\nHere's what changed because of it\nI'd like to talk about my salary",
                    "Полгода я веду запуск\nВот что за это время изменилось\nЯ хочу обсудить свою зарплату"),
                notSay = t("Everyone else gets paid more", "Всем остальным платят больше"),
                exit = t("Let's both think and come back to this on Friday.", "Давайте оба подумаем и вернёмся к этому в пятницу."),
                createdAt = now,
            )
        )
        dao.upsertRehearsal(
            Rehearsal(
                roleId = id("parents.grownup"),
                title = t("Telling Mum we won't come for New Year", "Сказать маме, что на Новый год не приедем"),
                day = day - 12,
                goal = t("Say it kindly and not apologise for it", "Сказать мягко и не оправдываться"),
                say = t("We love coming to you\nThis year we want to stay home with the kids\nLet's come on the second instead",
                    "Мы любим к тебе приезжать\nВ этом году хотим остаться дома с детьми\nДавай приедем второго"),
                notSay = t("You always make it a guilt trip", "Ты вечно давишь на жалость"),
                exit = t("I hear you're upset. Let's talk again tomorrow.", "Я слышу, что ты расстроилась. Давай вернёмся к этому завтра."),
                outcome = t("She was hurt for a minute, then asked what to cook on the second.", "Минуту обижалась, потом спросила, что приготовить второго."),
                outcomeScore = 85,
                done = true,
                createdAt = at(day - 14, 12 * 60),
            )
        )

        dao.upsertThreshold(Threshold(roleId = id("work.craftsman"), at = 9 * 60, days = 0b0011111, label = t("Before work", "Перед работой")))
        dao.upsertThreshold(Threshold(roleId = id("home.harbor"), at = 18 * 60 + 30, days = 0b0011111, label = t("Coming home", "Дорога домой")))
        dao.upsertThreshold(Threshold(roleId = id("kids.playmate"), at = 19 * 60 + 15, label = t("Playtime", "Время игр")))

        dao.upsertNote(
            Note(
                kind = "LETTER", roleId = id("home.harbor"), title = t("From Quiet Harbor", "От Тихой гавани"), day = day - 5, createdAt = at(day - 5, 22 * 60),
                body = JSONObject().put(
                    "text",
                    t(
                        "Anna, I see how hard you try, and I see the evenings you come home still at work in your head. You don't need to be perfect at the door. Three breaths, the phone in the bag, and ask them one real question. That's all I ask of you. The rest will follow.",
                        "Аня, я вижу, как ты стараешься, и вижу вечера, когда ты приходишь домой, а голова всё ещё на работе. Не нужно быть идеальной на пороге. Три вдоха, телефон в сумку и один настоящий вопрос. Больше я ничего не прошу. Остальное приложится.",
                    ),
                ).toString(),
            )
        )
        dao.upsertNote(
            Note(
                kind = "OTHERS", title = t("On my best day", "В мой лучший день"), day = day - 9, createdAt = at(day - 9, 21 * 60),
                body = JSONObject().put(
                    "rows",
                    JSONArray()
                        .put(JSONObject().put("who", t("Misha, my son", "Миша, сын")).put("text", t("Mum plays with me and doesn't look at her phone.", "Мама играет со мной и не смотрит в телефон.")))
                        .put(JSONObject().put("who", t("Dima", "Дима")).put("text", t("She listens to the end and laughs at my jokes.", "Дослушивает до конца и смеётся над моими шутками.")))
                        .put(JSONObject().put("who", t("My team", "Команда")).put("text", t("Clear, fair, and calm when things burn.", "Понятная, справедливая и спокойная, когда всё горит."))),
                ).toString(),
            )
        )
        dao.upsertNote(
            Note(
                kind = "COUNCIL", title = t("Take the new project or keep Saturdays free?", "Брать новый проект или оставить субботы свободными?"),
                day = day - 3, createdAt = at(day - 3, 21 * 60),
                body = JSONObject()
                    .put(
                        "answers",
                        JSONArray()
                            .put(JSONObject().put("roleId", id("work.craftsman")).put("text", t("It's a great project and I'd grow.", "Проект отличный, я бы на нём выросла.")))
                            .put(JSONObject().put("roleId", id("partner.ally")).put("text", t("Saturdays are the only time we have just for us.", "Субботы — единственное время только для нас.")))
                            .put(JSONObject().put("roleId", id("self.friend")).put("text", t("You're tired. Don't add more until you rest.", "Ты устала. Не бери больше, пока не отдохнёшь."))),
                    )
                    .put("core", t("Warmth and growth both matter. Growth can wait a quarter.", "Важны и тепло, и рост. Рост может подождать квартал."))
                    .put("decision", t("Say no for now, ask to join in spring.", "Пока отказаться, попроситься в проект весной."))
                    .toString(),
            )
        )
    }
}
