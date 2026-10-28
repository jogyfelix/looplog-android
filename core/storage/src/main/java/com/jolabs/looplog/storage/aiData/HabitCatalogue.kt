package com.jolabs.looplog.storage.aiData

object HabitCatalogue {
    // Structure: Map<CategoryName, List<Pair<ID, Description>>>
    val ALL: Map<String, List<Pair<String, String>>> = mapOf(

        "sleep" to listOf(
            "sleep_before_11"       to "Sleep before 11 PM",
            "no_screen_1hr"         to "No screens 1 hr before bed",
            "consistent_wake_time"  to "Wake up at the same time daily",
            "evening_reading"       to "Read before bed",
            "wind_down_routine"     to "Follow a wind-down routine",
            "no_caffeine_5pm"       to "No caffeine after 5 PM",
            "dark_cool_room"        to "Keep room dark and cool",
            "no_nap_after_3pm"      to "No naps after 3 PM"
        ),

        "fitness" to listOf(
            "walk_30"               to "Walk 30 minutes",
            "morning_stretch"       to "Morning stretching",
            "steps_8k"              to "Hit 8,000 steps",
            "workout_3x_week"       to "Work out 3× a week",
            "take_stairs"           to "Take the stairs",
            "active_break"          to "Take active breaks every hour",
            "evening_walk"          to "Evening walk after dinner",
            "yoga_15"               to "15 min yoga or mobility work"
        ),

        "hydration" to listOf(
            "water_morning"         to "Drink water first thing in the morning",
            "water_8_glasses"       to "Drink 8 glasses of water",
            "water_before_meals"    to "Drink water before each meal",
            "no_sugary_drinks"      to "Replace sugary drinks with water",
            "carry_water_bottle"    to "Carry a water bottle",
            "herbal_tea_evening"    to "Herbal tea in the evening"
        ),

        "nutrition" to listOf(
            "eat_breakfast"         to "Eat a proper breakfast",
            "home_cooked_meal"      to "Cook at least one meal at home",
            "no_late_night_eating"  to "No eating after 9 PM",
            "fruit_daily"           to "Eat a fruit every day",
            "veggies_every_meal"    to "Include vegetables in every meal",
            "mindful_eating"        to "Eat without screens",
            "meal_prep_sunday"      to "Meal prep on Sundays",
            "no_junk_weekdays"      to "Avoid junk food on weekdays"
        ),

        "mindfulness" to listOf(
            "meditate_10"           to "Meditate for 10 minutes",
            "deep_breathing"        to "Practice deep breathing",
            "gratitude_journal"     to "Write 3 things you're grateful for",
            "digital_detox_1hr"     to "1 hr digital detox daily",
            "morning_silence"       to "5 min of silence in the morning",
            "nature_walk"           to "Spend time in nature",
            "no_phone_mornings"     to "No phone for first 30 min after waking"
        ),

        "productivity" to listOf(
            "plan_day_morning"      to "Plan your day every morning",
            "deep_work_block"       to "1 hr deep work block",
            "no_phone_work_block"   to "Phone-free work sessions",
            "weekly_review"         to "Weekly review every Sunday",
            "inbox_zero"            to "Clear inbox daily",
            "single_task"           to "Single-task, no multitasking",
            "most_important_task"   to "Tackle the most important task first",
            "end_of_day_shutdown"   to "End-of-day shutdown ritual"
        ),

        "learning" to listOf(
            "read_15"               to "Read for 15 minutes",
            "podcast_commute"       to "Listen to a podcast while commuting",
            "learn_one_thing"       to "Learn one new thing daily",
            "flashcard_review"      to "Review flashcards",
            "write_summary"         to "Summarise what you learned today",
            "online_course_30"      to "30 min online course",
            "teach_someone"         to "Explain something you learned"
        ),

        "digital_wellness" to listOf(
            "screen_time_limit"     to "Set a daily screen time limit",
            "no_social_morning"     to "No social media in the morning",
            "app_timer"             to "Use app timers for social media",
            "phone_free_meals"      to "Phone-free mealtimes",
            "no_phone_bed"          to "No phone in bed",
            "grayscale_mode"        to "Use grayscale mode after 9 PM"
        ),

        "finance" to listOf(
            "track_spending"        to "Log daily expenses",
            "no_impulse_buy"        to "Wait 24 hrs before any purchase",
            "save_first"            to "Transfer savings on payday",
            "review_subscriptions"  to "Review subscriptions monthly",
            "pack_lunch"            to "Pack lunch instead of eating out",
            "weekly_budget_check"   to "Weekly budget check-in"
        ),

        "social" to listOf(
            "reach_out_friend"      to "Reach out to a friend or family member",
            "gratitude_message"     to "Send a gratitude message",
            "active_listening"      to "Practice active listening today",
            "journal_5min"          to "Journal for 5 minutes",
            "compliment_someone"    to "Give a genuine compliment",
            "family_dinner"         to "Have dinner with family"
        ),

        "music" to listOf(
            "practice_instrument"   to "Practice instrument for 20 min",
            "music_theory_15"       to "15 min music theory study",
            "listen_mindfully"      to "Listen to music without distractions",
            "learn_new_piece"       to "Work on a new piece",
            "record_practice"       to "Record a practice session"
        ),

        "environment" to listOf(
            "declutter_5min"        to "Declutter for 5 minutes",
            "make_bed"              to "Make your bed every morning",
            "clean_desk"            to "Keep desk clean before sleeping",
            "plant_care"            to "Water or tend to plants",
            "weekly_clean"          to "Weekly home clean",
            "reduce_plastic"        to "Avoid single-use plastic today"
        ),

        "creativity" to listOf(
            "sketch_10"               to "Sketch or doodle for 10 minutes",
            "free_write_10"           to "Free-write without editing for 10 minutes",
            "creative_project_30"     to "Spend 30 min on a personal creative project",
            "brainstorm_ideas"        to "Write down 10 new ideas on anything",
            "consume_for_inspiration" to "Read, watch, or listen purely for creative fuel",
            "make_something"          to "Build or make something with your hands",
            "photo_walk"              to "Take a photography walk"
        ),
        "quit" to listOf(
            "no_alcohol"            to "No alcohol today",
            "no_smoking"            to "No cigarettes or vaping",
            "no_junk_food"          to "No junk or processed food",
            "under_2hr_screen"      to "Under 2 hrs of recreational screen time",
            "no_online_shopping"    to "No impulse online shopping",
            "no_caffeine_today"     to "Caffeine-free day"
        ),
    )

    fun forCategory(category: String): List<Pair<String, String>> =
        ALL[category] ?: emptyList()

    fun allIds(): Set<String> =
        ALL.values.flatMap { list -> list.map { it.first } }.toSet()

    fun findById(id: String): Pair<String, String>? =
        ALL.values.asSequence().flatMap { it }.find { it.first == id }
}
