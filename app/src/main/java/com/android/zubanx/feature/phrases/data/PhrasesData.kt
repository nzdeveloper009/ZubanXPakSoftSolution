package com.android.zubanx.feature.phrases.data

object PhrasesData {

    val categories: List<PhraseCategory> = listOf(
        PhraseCategory.Dining,
        PhraseCategory.Emergency,
        PhraseCategory.Travel,
        PhraseCategory.Greeting,
        PhraseCategory.Shopping,
        PhraseCategory.Hotel,
        PhraseCategory.Office,
        PhraseCategory.Trouble,
        PhraseCategory.Entertainment,
        PhraseCategory.Medicine
    )

    fun categoryById(id: String): PhraseCategory =
        categories.first { it.id == id }

    val phrases: Map<PhraseCategory, List<String>> = mapOf(
        PhraseCategory.Dining to listOf(
            "A table for two, please.",
            "Can I see the menu?",
            "I am allergic to nuts.",
            "What do you recommend?",
            "Could we have the bill, please?",
            "Is there a vegetarian option?",
            "No spice, please.",
            "Water, please.",
            "This is delicious!",
            "Can I have a takeaway?"
        ),
        PhraseCategory.Emergency to listOf(
            "Call an ambulance!",
            "I need a doctor.",
            "I have lost my passport.",
            "Call the police, please.",
            "There is a fire!",
            "I have been robbed.",
            "I need help.",
            "Where is the nearest hospital?",
            "I am injured.",
            "Please hurry!"
        ),
        PhraseCategory.Travel to listOf(
            "Where is the bus station?",
            "How much is the ticket?",
            "Does this bus go to the city centre?",
            "Can you call me a taxi?",
            "Where is the airport?",
            "I missed my flight.",
            "I need to go to this address.",
            "Is this the right platform?",
            "How far is it?",
            "Can I have a map?"
        ),
        PhraseCategory.Greeting to listOf(
            "Good morning!",
            "Good evening!",
            "How are you?",
            "Nice to meet you.",
            "My name is ...",
            "I do not speak this language.",
            "Do you speak English?",
            "Thank you very much.",
            "You are welcome.",
            "Goodbye!"
        ),
        PhraseCategory.Shopping to listOf(
            "How much does this cost?",
            "Do you have a smaller size?",
            "Can I try this on?",
            "I would like to buy this.",
            "Do you accept cards?",
            "Can I get a discount?",
            "Where is the fitting room?",
            "I am just looking.",
            "Can I get a receipt?",
            "Do you have this in another colour?"
        ),
        PhraseCategory.Hotel to listOf(
            "I have a reservation.",
            "I would like to check in.",
            "What time is checkout?",
            "Can I have an extra pillow?",
            "The air conditioning is not working.",
            "Can I have a wake-up call at 7?",
            "Where is the elevator?",
            "Can I have room service?",
            "I would like to extend my stay.",
            "Can you store my luggage?"
        ),
        PhraseCategory.Office to listOf(
            "I have a meeting at 10.",
            "Can I use the Wi-Fi?",
            "Where is the conference room?",
            "I need to print a document.",
            "Can I speak to the manager?",
            "I am here for an interview.",
            "Please send me the report.",
            "The projector is not working.",
            "I need a pen and paper.",
            "Can you reschedule the meeting?"
        ),
        PhraseCategory.Trouble to listOf(
            "I am lost.",
            "Can you help me?",
            "I do not understand.",
            "Please speak slowly.",
            "Can you write that down?",
            "I need a translator.",
            "This is not what I ordered.",
            "There is a problem with my room.",
            "I want to make a complaint.",
            "Can I speak to a supervisor?"
        ),
        PhraseCategory.Entertainment to listOf(
            "Two tickets, please.",
            "What time does the show start?",
            "Where is the entrance?",
            "Is there a student discount?",
            "Can I take photos here?",
            "Where is the nearest cinema?",
            "What is showing tonight?",
            "I would like to book in advance.",
            "Is this show suitable for children?",
            "Can I get a programme?"
        ),
        PhraseCategory.Medicine to listOf(
            "I need a pharmacy.",
            "I have a headache.",
            "I feel nauseous.",
            "I am diabetic.",
            "I am allergic to penicillin.",
            "I need my prescription filled.",
            "How many times a day should I take this?",
            "Do you have pain relief?",
            "I have a fever.",
            "I need to see a doctor urgently."
        )
    )
}
