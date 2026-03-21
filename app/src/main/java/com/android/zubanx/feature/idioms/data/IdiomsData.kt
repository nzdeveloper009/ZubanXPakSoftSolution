package com.android.zubanx.feature.idioms.data

object IdiomsData {

    val idioms: Map<IdiomCategory, List<IdiomEntry>> = mapOf(
        IdiomCategory.Common to listOf(
            IdiomEntry("Break a leg",             "Good luck",                                      "You're going on stage. Break a leg!"),
            IdiomEntry("Bite the bullet",         "Endure something painful or unpleasant",         "I'll just bite the bullet and finish this report tonight."),
            IdiomEntry("Hit the nail on the head","Describe exactly what is causing a situation",   "You hit the nail on the head with your diagnosis."),
            IdiomEntry("Under the weather",       "Feeling ill or unwell",                          "I'm a bit under the weather today — skipping the gym."),
            IdiomEntry("Break the ice",           "Say or do something to relieve tension",         "He told a joke to break the ice at the meeting."),
            IdiomEntry("A piece of cake",         "Something very easy",                            "The exam was a piece of cake."),
            IdiomEntry("Let the cat out of the bag","Accidentally reveal a secret",                 "She let the cat out of the bag about the surprise party."),
            IdiomEntry("Once in a blue moon",     "Very rarely",                                    "She visits us once in a blue moon."),
            IdiomEntry("Spill the beans",         "Reveal secret information",                      "Come on, spill the beans — what happened?"),
            IdiomEntry("Burn bridges",            "Permanently damage a relationship",              "Don't burn bridges with your old employer.")
        ),
        IdiomCategory.Business to listOf(
            IdiomEntry("Hit the ground running",  "Start something with great energy from the start","The new hire hit the ground running on day one."),
            IdiomEntry("Think outside the box",   "Think creatively and unconventionally",          "We need to think outside the box to solve this."),
            IdiomEntry("Touch base",              "Make brief contact with someone",                "Let's touch base on Monday to check progress."),
            IdiomEntry("Move the needle",         "Make a significant impact on something",         "This campaign really moved the needle on sales."),
            IdiomEntry("Low-hanging fruit",       "Easy tasks that can be achieved quickly",        "Let's pick the low-hanging fruit first."),
            IdiomEntry("On the same page",        "In agreement or mutual understanding",           "Let's make sure we're all on the same page."),
            IdiomEntry("Cut corners",             "Do something cheaply or carelessly",             "Never cut corners on safety procedures."),
            IdiomEntry("Back to the drawing board","Start over from the beginning",                 "The plan failed. It's back to the drawing board."),
            IdiomEntry("Get the ball rolling",    "Start something",                                "Let's get the ball rolling on the new project."),
            IdiomEntry("Raise the bar",           "Set a higher standard",                          "With that launch, they raised the bar for the whole industry.")
        ),
        IdiomCategory.Food to listOf(
            IdiomEntry("Bite off more than you can chew","Take on more than you can handle",        "He bit off more than he could chew with three projects."),
            IdiomEntry("Bring home the bacon",    "Earn money for the family",                      "She brings home the bacon while he raises the kids."),
            IdiomEntry("Butter someone up",       "Flatter someone to get something",               "He buttered up his boss before asking for a raise."),
            IdiomEntry("Couch potato",            "A lazy person who watches too much TV",          "Don't be a couch potato — go for a walk!"),
            IdiomEntry("In a nutshell",           "In summary",                                     "In a nutshell, the project failed because of poor planning."),
            IdiomEntry("Spill the milk",          "Worry about a past mistake that can't be fixed", "Don't cry over spilled milk — just move on."),
            IdiomEntry("Take with a grain of salt","View something skeptically",                    "Take those reviews with a grain of salt."),
            IdiomEntry("Full of beans",           "Very lively and energetic",                      "The kids are full of beans today."),
            IdiomEntry("Bread and butter",        "Someone's main source of income",                "Teaching is her bread and butter."),
            IdiomEntry("Cry over spilled milk",   "Be upset about something that cannot be changed","There's no point crying over spilled milk.")
        ),
        IdiomCategory.Animals to listOf(
            IdiomEntry("Barking up the wrong tree","Looking in the wrong place or accusing the wrong person","You're barking up the wrong tree — it wasn't me."),
            IdiomEntry("The elephant in the room","An obvious problem no one wants to discuss",    "Budget cuts are the elephant in the room."),
            IdiomEntry("Let sleeping dogs lie",   "Avoid bringing up old problems",                "Just let sleeping dogs lie and move on."),
            IdiomEntry("A wolf in sheep's clothing","A dangerous person pretending to be harmless","Watch out — he's a wolf in sheep's clothing."),
            IdiomEntry("Hold your horses",        "Wait or slow down",                              "Hold your horses — let me finish my sentence."),
            IdiomEntry("Kill two birds with one stone","Accomplish two things with one action",    "I'll kill two birds with one stone by shopping on my way home."),
            IdiomEntry("The cat's meow",          "Something or someone outstanding",              "This new phone is the cat's meow."),
            IdiomEntry("Straight from the horse's mouth","Directly from the original source",     "I heard it straight from the horse's mouth."),
            IdiomEntry("Smell a rat",             "Suspect something is wrong",                    "I smell a rat — something doesn't add up."),
            IdiomEntry("Fish out of water",       "Someone in an unfamiliar situation",            "At the tech conference, she felt like a fish out of water.")
        ),
        IdiomCategory.Time to listOf(
            IdiomEntry("Against the clock",       "In a hurry with very little time",              "We're working against the clock to meet the deadline."),
            IdiomEntry("A stitch in time",        "Act promptly to prevent a bigger problem later","Fix the leak now — a stitch in time saves nine."),
            IdiomEntry("On the dot",              "Exactly on time",                               "The meeting started at 9 on the dot."),
            IdiomEntry("In the nick of time",     "Just in time; at the last moment",              "We caught the train in the nick of time."),
            IdiomEntry("Time flies",              "Time passes quickly",                           "Time flies when you're having fun."),
            IdiomEntry("Beat around the bush",    "Avoid talking about the main topic",            "Stop beating around the bush and tell me the problem."),
            IdiomEntry("Money doesn't grow on trees","Money is not easily obtained",              "Don't waste it — money doesn't grow on trees."),
            IdiomEntry("Penny pincher",           "A very frugal person",                          "He's such a penny pincher — he never tips."),
            IdiomEntry("Cost an arm and a leg",   "Be very expensive",                             "That car costs an arm and a leg."),
            IdiomEntry("Saved for a rainy day",   "Kept for a time of need",                       "I've saved some money for a rainy day.")
        ),
        IdiomCategory.Weather to listOf(
            IdiomEntry("Under the weather",       "Feeling ill",                                   "I'm under the weather today."),
            IdiomEntry("On thin ice",             "In a risky situation",                          "He's on thin ice with the manager after being late again."),
            IdiomEntry("Steal someone's thunder", "Take attention away from someone",              "She stole his thunder by announcing it first."),
            IdiomEntry("Every cloud has a silver lining","Something positive in every bad situation","Losing the job led to a better opportunity — every cloud has a silver lining."),
            IdiomEntry("Storm in a teacup",       "A big fuss about something trivial",            "The argument was just a storm in a teacup."),
            IdiomEntry("Chase rainbows",          "Pursue unrealistic dreams",                     "Stop chasing rainbows and focus on achievable goals."),
            IdiomEntry("A ray of sunshine",       "Something or someone who brings happiness",     "Her smile is a ray of sunshine."),
            IdiomEntry("Snowed under",            "Having too much work",                          "I'm snowed under with assignments this week."),
            IdiomEntry("Blow hot and cold",       "Keep changing your opinion",                    "He keeps blowing hot and cold about the proposal."),
            IdiomEntry("Come rain or shine",      "No matter what the circumstances",              "I'll be there come rain or shine.")
        ),
        IdiomCategory.Emotions to listOf(
            IdiomEntry("On cloud nine",           "Extremely happy",                               "She's been on cloud nine since getting the promotion."),
            IdiomEntry("Hit rock bottom",         "Reach the lowest point",                        "After losing his job, he hit rock bottom."),
            IdiomEntry("Wear your heart on your sleeve","Openly show your emotions",              "She wears her heart on her sleeve — you always know how she feels."),
            IdiomEntry("Down in the dumps",       "Feeling sad and depressed",                     "He's been down in the dumps since the breakup."),
            IdiomEntry("Have butterflies",        "Feel nervous or excited",                       "I always have butterflies before a presentation."),
            IdiomEntry("Blow off steam",          "Release stress or anger",                       "He goes running to blow off steam after work."),
            IdiomEntry("Over the moon",           "Extremely pleased",                             "She was over the moon when she got the scholarship."),
            IdiomEntry("Get cold feet",           "Become nervous or hesitant about something",   "He got cold feet before the wedding."),
            IdiomEntry("Bite someone's head off", "Respond angrily to someone",                   "Don't bite my head off — I was just asking!"),
            IdiomEntry("Keep your chin up",       "Stay positive during difficult times",          "Keep your chin up — things will get better.")
        ),
        IdiomCategory.Travel to listOf(
            IdiomEntry("In the same boat",        "Facing the same problems as others",            "We're all in the same boat during this crisis."),
            IdiomEntry("Miss the boat",           "Miss an opportunity",                           "He missed the boat by not investing early."),
            IdiomEntry("Smooth sailing",          "A situation with no problems",                  "After fixing the bug, it was smooth sailing."),
            IdiomEntry("Off the beaten path",     "Away from popular areas; unusual",              "They prefer off-the-beaten-path travel destinations."),
            IdiomEntry("Blaze a trail",           "Be the first to do something new",              "She blazed a trail as the first woman in the role."),
            IdiomEntry("Hit the road",            "Leave or start a journey",                      "We need to hit the road by 6 AM to avoid traffic."),
            IdiomEntry("On the right track",      "Doing something correctly",                     "You're on the right track with your approach."),
            IdiomEntry("Go the extra mile",       "Make more effort than is expected",             "She always goes the extra mile for her students."),
            IdiomEntry("Lost in translation",     "Meaning changed or lost when communicated",     "The joke was lost in translation."),
            IdiomEntry("Arrive in style",         "Come in an impressive manner",                  "They arrived in a vintage car.")
        )
    )

    fun categoryById(id: String): IdiomCategory = IdiomCategory.fromId(id)
    fun idiomsByCategory(category: IdiomCategory): List<IdiomEntry> = idioms[category] ?: emptyList()
}
