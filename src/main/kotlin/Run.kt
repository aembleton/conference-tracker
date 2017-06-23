import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.io.File
import java.util.*

/**
 * Created by embletona on 30/11/2016.
 */

fun main(args: Array<String>) {
    val talks = getTalks("talks.txt").map { it.first to it.second }.toMap()
    printTracks(talks)
}

fun printTracks(talks:Map<String, Int>, track:Int=1) {

    if (talks.isEmpty()) {
        return
    }

    println()
    println("# Track $track")
    println()
    val talksToPrint = createTrack(talks)
    talksToPrint.forEach { println("${it.first} ${it.second}") }
    println()
    printTracks(talks.filterNot { talksToPrint.map { it.second }.contains(it.key) }, track + 1)
}

fun getTalks(fileName:String) = File(fileName).readLines().map {
    val title = it.split(" ").dropLast(1).reduce { s1, s2 -> "$s1 $s2" }
    val lengthOfTalk = it.split(" ").takeLast(1).first()
    val time = if (lengthOfTalk == "lightning") {
        5
    } else {
        lengthOfTalk.replace("min","").toInt()
    }
    Pair(title, time)
}

fun createTrack(talks:Map<String, Int>):List<Pair<String, String>> {
    val morning = createSession(talks, 180, 180)
    val afternoonTalks = talks.filterNot { morning.contains(it.key) }
    val afternoon = createSession(afternoonTalks, 180, 240)

    val morningStart = DateTime.now().withHourOfDay(9).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
    val decoratedMorning = decorateWithTimeStamps(talks, morning, morningStart).reversed()

    val afternoonStart = DateTime.now().withHourOfDay(13).withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0)
    val decoratedAfternoon = decorateWithTimeStamps(talks, afternoon, afternoonStart).reversed()

    val lunch = listOf(Pair("12:00PM", "Lunch"))

    val afternoonLength = talks.filter { afternoon.contains(it.key) }.values.sum()
    val networkingStart = afternoonStart.plusMinutes(afternoonLength)
    val fmt = DateTimeFormat.forPattern("KK:mmaa")
    val timeStamp = fmt.print(networkingStart)
    val networking = listOf(Pair(timeStamp, "Networking"))

    return decoratedMorning + lunch + decoratedAfternoon + networking
}

fun createSession(talks:Map<String, Int>, minLength:Int, maxLength:Int):List<String> {

    val sortedTalks = talks.toList().sortedBy { it.second }.reversed()
    val session = getTalks(minLength, maxLength, sortedTalks, sortedTalks)
    return session.map { it.first }
}

tailrec fun decorateWithTimeStamps(allTalks:Map<String, Int>, talksToDecorate:List<String>, startTime:DateTime): List<Pair<String,String>> {
    if (talksToDecorate.isEmpty()) {
        return emptyList()
    }

    val talkName = talksToDecorate.first()
    val talkTime = allTalks.get(talkName) ?: 0
    val newStartTime = startTime.plusMinutes(talkTime)
    val fmt = DateTimeFormat.forPattern("KK:mmaa")
    val timeStamp = fmt.print(startTime)
    return decorateWithTimeStamps(allTalks, talksToDecorate.drop(1), newStartTime) + Pair(timeStamp, talkName)
}

tailrec fun getTalks(min:Int, max:Int, initial:List<Pair<String,Int>>, remaining:List<Pair<String,Int>>, track:List<Pair<String,Int>> = emptyList()):List<Pair<String,Int>> {
    val length = track.sumBy { it.second }

    if (length in min..max) {
        return track
    }

    val remainder = max - length

    val talksToAdd = remaining.filter { it.second <= remainder }

    if (talksToAdd.isNotEmpty()) {
        val talkToAdd = talksToAdd.first()
        val newRemaining = remaining - talkToAdd
        val newTack = track + talkToAdd
        return getTalks(min, max, initial, newRemaining, newTack)
    }

    if (track.size > 1) {
        val newTrack = track.dropLast(1)
        return getTalks(min, max, initial, remaining, newTrack)
    }

    val newRemaining = initial - track[0]
    val newTrack = LinkedList<Pair<String, Int>>()
    return getTalks(min, max, initial, newRemaining, newTrack)
}