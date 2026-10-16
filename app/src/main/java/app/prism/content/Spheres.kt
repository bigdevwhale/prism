package app.prism.content

/**
 * The preset spheres, in the order they are offered. [icon] is an [app.prism.ui.SphereIcon] name,
 * [hue] an index into the spectrum of [app.prism.ui.theme.Palette.hues].
 */
class SpherePreset(val key: String, val icon: String, val hue: Int)

val SpherePresets = listOf(
    SpherePreset("home", "HOME", 3),
    SpherePreset("partner", "HEART", 0),
    SpherePreset("kids", "CHILD", 4),
    SpherePreset("parents", "ELDER", 2),
    SpherePreset("kin", "FAMILY", 1),
    SpherePreset("friends", "FRIENDS", 5),
    SpherePreset("work", "WORK", 9),
    SpherePreset("lead", "FLAG", 10),
    SpherePreset("deals", "HANDSHAKE", 8),
    SpherePreset("study", "BOOK", 7),
    SpherePreset("create", "BRUSH", 11),
    SpherePreset("body", "RUN", 6),
    SpherePreset("money", "COIN", 3),
    SpherePreset("self", "SELF", 10),
    SpherePreset("meaning", "SPARK", 11),
    SpherePreset("community", "CITY", 8),
    SpherePreset("strangers", "WAVE", 5),
    SpherePreset("conflict", "STORM", 0),
    SpherePreset("hard", "ANCHOR", 9),
    SpherePreset("play", "PLAY", 4),
    SpherePreset("online", "PHONE", 7),
    SpherePreset("care", "CARE", 1),
)

/** Spheres switched on when the user skips choosing in onboarding. */
val DefaultSpheres = listOf("home", "partner", "work", "friends", "parents", "self", "conflict")

val SpheresEn = mapOf(
    "home" to SphereText("Home & family", "The people you share a roof and a kitchen with."),
    "partner" to SphereText("Partner", "The one you chose, and who chose you."),
    "kids" to SphereText("Children", "Who they will remember you as."),
    "parents" to SphereText("Parents", "Being a grown-up next to the people who raised you."),
    "kin" to SphereText("Relatives", "Siblings, cousins, in-laws, the big family table."),
    "friends" to SphereText("Friends", "The family you pick yourself."),
    "work" to SphereText("Work", "Your craft and the people you do it with."),
    "lead" to SphereText("Leading", "When others look to you for direction."),
    "deals" to SphereText("Clients & deals", "Negotiations, sales, the other side of the table."),
    "study" to SphereText("Learning", "Courses, books, a new skill, being a beginner."),
    "create" to SphereText("Creating", "Making things that didn't exist before."),
    "body" to SphereText("Body & sport", "Training, sleep, food, how you treat your body."),
    "money" to SphereText("Money", "Earning, spending, saving, and how calm you are about it."),
    "self" to SphereText("Alone with yourself", "How you talk to yourself when nobody is watching."),
    "meaning" to SphereText("Meaning", "Faith, values, the bigger picture."),
    "community" to SphereText("Community", "Neighbours, your town, volunteering, the common good."),
    "strangers" to SphereText("New people", "First meetings, parties, small talk."),
    "conflict" to SphereText("Conflict & anger", "Who you are when you are hurt or furious."),
    "hard" to SphereText("Hard times", "Loss, illness, crisis: when the ground shakes."),
    "play" to SphereText("Rest & play", "Holidays, hobbies, fun for its own sake."),
    "online" to SphereText("Online", "Chats, comments, feeds, how you show up on a screen."),
    "care" to SphereText("Caring for someone", "Looking after someone who is ill, old or struggling."),
)

val SpheresRu = mapOf(
    "home" to SphereText("Дом и семья", "Те, с кем у тебя одна крыша и одна кухня."),
    "partner" to SphereText("Партнёр", "Человек, которого выбрал{|а} ты и который выбрал тебя."),
    "kids" to SphereText("Дети", "Каким тебя запомнят твои дети."),
    "parents" to SphereText("Родители", "Быть {взрослым|взрослой} рядом с теми, кто тебя вырастил."),
    "kin" to SphereText("Родня", "Братья, сёстры, тёти, свёкры и большой семейный стол."),
    "friends" to SphereText("Друзья", "Семья, которую выбираешь сам{|а}."),
    "work" to SphereText("Работа", "Твоё дело и люди, с которыми ты его делаешь."),
    "lead" to SphereText("Руководство", "Когда другие ждут от тебя направления."),
    "deals" to SphereText("Клиенты и переговоры", "Продажи, сделки, другая сторона стола."),
    "study" to SphereText("Учёба", "Курсы, книги, новый навык, роль новичка."),
    "create" to SphereText("Творчество", "Делать то, чего раньше не было."),
    "body" to SphereText("Тело и спорт", "Тренировки, сон, еда, отношение к своему телу."),
    "money" to SphereText("Деньги", "Заработать, потратить, отложить и не дёргаться."),
    "self" to SphereText("Наедине с собой", "Как ты говоришь с собой, когда никто не слышит."),
    "meaning" to SphereText("Смысл", "Вера, ценности, большая картина."),
    "community" to SphereText("Общество", "Соседи, город, волонтёрство, общее дело."),
    "strangers" to SphereText("Новые люди", "Первые встречи, вечеринки, разговоры ни о чём."),
    "conflict" to SphereText("Конфликт и гнев", "{Какой|Какая} ты, когда тебе больно или ты в ярости."),
    "hard" to SphereText("Трудные времена", "Потери, болезнь, кризис — когда земля уходит из-под ног."),
    "play" to SphereText("Отдых и игра", "Отпуск, хобби, удовольствие просто так."),
    "online" to SphereText("Онлайн", "Чаты, комментарии, ленты — {какой|какая} ты по ту сторону экрана."),
    "care" to SphereText("Забота о близком", "Когда ухаживаешь за тем, кто болеет, стареет или не справляется."),
)
