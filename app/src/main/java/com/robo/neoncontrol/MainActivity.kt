package com.robo.neoncontrol

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.OutputStream
import java.util.UUID
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private val bg = Color.rgb(5, 7, 11)
    private val panel = Color.rgb(14, 19, 27)
    private val neon = Color.rgb(0, 245, 212)
    private val blue = Color.rgb(67, 160, 255)
    private val white = Color.rgb(235, 245, 250)
    private val muted = Color.rgb(137, 153, 168)
    private val sppUuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var adapter: BluetoothAdapter? = null
    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var output: OutputStream? = null
    @Volatile private var readerActive = false
    private val sendLock = Any()
    private lateinit var status: TextView
    private lateinit var deviceLabel: TextView
    private lateinit var connectButton: Button
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = BluetoothAdapter.getDefaultAdapter()
        buildScreen()
        requestBtPermissionThenRefresh()
    }

    private fun buildScreen() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(30))
            setBackgroundColor(bg)
        }
        scroll.addView(root)
        setContentView(scroll)

        text(root, "ROBO", 34f, neon, true)
        text(root, "NEON CONTROL SYSTEM  //  ESP32 BLUETOOTH", 10f, muted, true)
        space(root, 16)
        val connectCard = card()
        root.addView(connectCard)
        deviceLabel = text(connectCard, "DEVICE  •  ROBO", 14f, white, true)
        status = text(connectCard, "● DISCONNECTED", 12f, Color.rgb(255, 90, 110), true)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        connectCard.addView(row)
        connectButton = button("CONNECT", neon) { chooseDevice() }
        row.addView(connectButton, LinearLayout.LayoutParams(0, dp(48), 1f).apply { rightMargin = dp(8) })
        val refresh = button("DEVICES", blue) { chooseDevice() }
        row.addView(refresh, LinearLayout.LayoutParams(0, dp(48), 1f))

        section("QUICK DASHBOARD", "Select a robot operating mode")
        grid(listOf(
            Triple("BLUETOOTH", "W", neon), Triple("MANUAL", "M", neon),
            Triple("AUTO", "A", blue), Triple("OBJECT FOLLOW", "O", blue),
            Triple("OBSTACLE AVOID", "P", blue), Triple("DANCE", "D", neon),
            Triple("SALUTE", "T", neon), Triple("HEAD CONTROL", "G", neon),
            Triple("RELAX MODE", "I", neon), Triple("GAME CENTER", "N", neon),
            Triple("VOICE MODE", "Y", blue), Triple("CHAT MODE", "U", blue),
            Triple("SINGING MODE", "Z", blue), Triple("SAFE / STOP", "X", Color.rgb(255, 85, 110))
        ))

        section("MANUAL DRIVE & SERVOS", "Select BLUETOOTH or MANUAL mode first. Tap the movement, hand and head controls; use STOP to halt the motors.")
        val drive = card(); root.addView(drive)
        val driveGrid = GridLayout(this).apply { columnCount = 3; rowCount = 3 }
        drive.addView(driveGrid)
        addGridButton(driveGrid, "↑\nFORWARD", "F", 1, 0, 1)
        addGridButton(driveGrid, "←\nLEFT", "L", 0, 1, 1)
        addGridButton(driveGrid, "■\nSTOP", "S", 1, 1, 1, Color.rgb(255, 85, 110))
        addGridButton(driveGrid, "→\nRIGHT", "R", 2, 1, 1)
        addGridButton(driveGrid, "↓\nBACK", "B", 1, 2, 1)
        val servoRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        drive.addView(servoRow)
        listOf("LEFT HAND ↑" to "H", "LEFT ↓" to "h", "RIGHT ↑" to "J", "RIGHT ↓" to "j", "BOTH ↑" to "K", "BOTH ↓" to "k").forEach { (label, cmd) ->
            servoRow.addView(button(label, blue) { send(cmd) }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), dp(8), dp(3), 0) })
        }
        val headRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        drive.addView(headRow)
        listOf("HEAD LEFT" to "Q", "CENTER" to "C", "HEAD RIGHT" to "E").forEach { (label, cmd) ->
            headRow.addView(button(label, neon) { send(cmd) }, LinearLayout.LayoutParams(0, dp(44), 1f).apply { setMargins(dp(3), dp(8), dp(3), 0) })
        }

        section("GAME CENTER  •  ALL 16 GAMES", "Choose any of the 16 games. Selection enters Game Center and launches the selected game.")
        actionGrid(listOf(
            Triple("01  MEMORY CHALLENGE", "!", neon),
            Triple("02  PONG PRACTICE", "@", neon),
            Triple("03  REACTION CHALLENGE", "#", neon),
            Triple("04  MINI MAZE EXPLORER", "\$", neon),
            Triple("05  SPACE GAME", "%", neon),
            Triple("06  DODGE GAME", "^", neon),
            Triple("07  SNAKE GAME", "&", neon),
            Triple("08  TIC-TAC-TOE", "*", neon),
            Triple("09  CATCH THE STAR", "(", neon),
            Triple("10  ROCKET RUNNER", ")", neon),
            Triple("11  TARGET PRACTICE", "_", neon),
            Triple("12  BOUNCING BALL", "+", neon),
            Triple("13  PIXEL INVADERS", "=", neon),
            Triple("14  ORBIT DEFENDER", "?", neon),
            Triple("15  LIGHT RUNNER", "~", neon),
            Triple("16  ASTEROID ESCAPE", ":", neon)
        )) { command -> sendSequence("N", command) }

        section("GAME CONTROL PAD", "2 Up • 4 Left • 6 Right • 8 Down • 5 Action/Fire. Tic-Tac-Toe uses its separate 1–9 board.")
        val game = card(); root.addView(game)
        text(game, "DIRECTIONAL CONTROLS", 12f, muted, true)
        val pad = GridLayout(this).apply { columnCount = 3; rowCount = 3; alignmentMode = GridLayout.ALIGN_BOUNDS }
        game.addView(pad)
        addGridButton(pad, "▲\n2", "2", 1, 0, 1)
        addGridButton(pad, "◀\n4", "4", 0, 1, 1)
        addGridButton(pad, "●\n5 ACTION", "5", 1, 1, 1, blue)
        addGridButton(pad, "▶\n6", "6", 2, 1, 1)
        addGridButton(pad, "▼\n8", "8", 1, 2, 1)
        val utilityRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        game.addView(utilityRow)
        utilityRow.addView(button("PAUSE / RESUME", blue) { send("S") }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), dp(8), dp(3), 0) })
        utilityRow.addView(button("EXIT / SAFE", Color.rgb(255, 85, 110)) { send("X") }, LinearLayout.LayoutParams(0, dp(46), 1f).apply { setMargins(dp(3), dp(8), dp(3), 0) })
        text(game, "TIC-TAC-TOE BOARD  •  SELECT A CELL", 12f, muted, true)
        val cells = GridLayout(this).apply { columnCount = 3 }
        game.addView(cells)
        for (i in 1..9) addGridButton(cells, i.toString(), i.toString(), (i-1)%3, (i-1)/3, 1)

        section("CONVERSATION & PERSONAL GREETINGS", "Select CHAT MODE first, then use these 20 phrase triggers and Dad / Mom / Friend greetings.")
        val chat = card(); root.addView(chat)
        val chatGrid = GridLayout(this).apply { columnCount = 3 }
        chat.addView(chatGrid)
        val phrases = listOf("MY NAME" to "1", "ASSISTANT" to "2", "WHO ARE YOU?" to "3", "DOING GREAT" to "4", "NICE TO MEET" to "5", "HOW CAN I HELP" to "6", "LISTENING" to "7", "YES" to "8", "SORRY" to "9", "REPEAT" to "0", "WHAT IS NAME?" to "A", "WHO CREATED?" to "B", "CREATOR" to "C", "WHAT CAN YOU DO?" to "D", "MOVE & INTERACT" to "E", "LIKE MUSIC?" to "F", "LOVE MUSIC" to "G", "HAPPY?" to "H", "READY TO HELP" to "I", "GOODBYE" to "J", "DAD GREETING" to "K", "MOM GREETING" to "L", "FRIEND GREETING" to "M")
        phrases.forEach { (label, cmd) -> chatGrid.addView(button(label, blue) { send(cmd) }, GridLayout.LayoutParams().apply { width = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt() - 74) / 3; height = dp(48); setMargins(dp(3), dp(3), dp(3), dp(3)) }) }

        section("VOICE COMMAND ACTIONS", "Select VOICE MODE first. These buttons send the actions accepted by the current firmware.")
        val voice = card(); root.addView(voice)
        val voiceGrid = GridLayout(this).apply { columnCount = 2 }
        voice.addView(voiceGrid)
        listOf(
            "SALUTE" to "S", "DANCE" to "D", "HEAD LEFT" to "L",
            "HEAD RIGHT" to "R", "HEAD CENTER" to "C", "FORWARD" to "F", "BACKWARD" to "B"
        ).forEach { (label, cmd) ->
            voiceGrid.addView(button(label, neon) { send(cmd) }, GridLayout.LayoutParams().apply {
                width = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt() - 76) / 2
                height = dp(48); setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }

        section("AUDIO & DANCE", "Singing Mode randomly plays tracks 0098–0107. Dance Mode plays random songs and uses the GPIO19 microphone sound pulses to trigger hand/head moves; it does not dance on a timer without detected sound. Games never use these songs as background music.")
        val audio = card(); root.addView(audio)
        audio.addView(button("ACTIVATE SINGING MODE", blue) { send("Z") })
        audio.addView(button("ACTIVATE SOUND-REACTIVE DANCE", neon) { send("D") })

        section("SYSTEM", "Connection status and emergency control")
        val sys = card(); root.addView(sys)
        text(sys, "Bluetooth Classic • Serial Port Profile (SPP) • 9600 is not required for the phone link; ESP32 BluetoothSerial handles SPP.", 12f, muted, false)
        val safe = button("EMERGENCY STOP  /  SAFE MODE", Color.rgb(255, 75, 100)) { send("X") }
        sys.addView(safe)
        text(root, "ROBO NEON CONTROL  •  COMPLETE ACTION PANEL 1.2", 10f, muted, true).gravity = Gravity.CENTER
    }

    private fun section(title: String, subtitle: String) {
        space(root, 20); text(root, title, 16f, neon, true); text(root, subtitle, 11f, muted, false); space(root, 8)
    }

    private fun grid(items: List<Triple<String, String, Int>>) {
        val c = card(); root.addView(c)
        val g = GridLayout(this).apply { columnCount = 2 }
        c.addView(g)
        items.forEach { (label, cmd, color) ->
            g.addView(button(label, color) { send(cmd) }, GridLayout.LayoutParams().apply { width = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt() - 76) / 2; height = dp(52); setMargins(dp(4), dp(4), dp(4), dp(4)) })
        }
    }

    private fun actionGrid(items: List<Triple<String, String, Int>>, action: (String) -> Unit) {
        val c = card(); root.addView(c)
        val g = GridLayout(this).apply { columnCount = 2 }
        c.addView(g)
        items.forEach { (label, command, color) ->
            g.addView(button(label, color) { action(command) }, GridLayout.LayoutParams().apply {
                width = ((resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt() - 76) / 2
                height = dp(58)
                setMargins(dp(4), dp(4), dp(4), dp(4))
            })
        }
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(12), dp(12), dp(12), dp(12))
        background = rounded(panel, 18, Color.rgb(34, 54, 65), 1)
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) }
    }

    private fun text(parent: LinearLayout, value: String, size: Float, color: Int, bold: Boolean): TextView {
        val t = TextView(this).apply {
            text = value; textSize = size; setTextColor(color)
            if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.06f
            setPadding(dp(4), dp(5), dp(4), dp(5))
        }
        parent.addView(t)
        return t
    }

    private fun button(label: String, accent: Int, action: () -> Unit): Button = Button(this).apply {
        text = label; textSize = 10f; isAllCaps = true; setTextColor(accent)
        background = rounded(Color.rgb(18, 27, 37), 12, accent, 1)
        setPadding(dp(4), dp(3), dp(4), dp(3))
        setOnClickListener { action() }
    }

    private fun addGridButton(g: GridLayout, label: String, cmd: String, col: Int, row: Int, span: Int, accent: Int = neon) {
        val b = button(label, accent) { send(cmd) }
        g.addView(b, GridLayout.LayoutParams(GridLayout.spec(row), GridLayout.spec(col)).apply { width = dp(92); height = dp(64); setMargins(dp(4), dp(4), dp(4), dp(4)) })
    }

    private fun rounded(color: Int, radius: Int, stroke: Int, width: Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat(); setStroke(dp(width), stroke)
    }

    private fun space(parent: LinearLayout, h: Int) { parent.addView(View(this), LinearLayout.LayoutParams(1, dp(h))) }
    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun requestBtPermissionThenRefresh() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_CONNECT), 41)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 41 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) refreshBluetoothState()
    }

    private fun refreshBluetoothState() {
        try {
            if (adapter == null) { setStatus("Bluetooth not supported", false); return }
            if (adapter?.isEnabled != true) setStatus("Turn on phone Bluetooth", false)
        } catch (_: SecurityException) { setStatus("Bluetooth permission required", false) }
    }

    private fun chooseDevice() {
        if (Build.VERSION.SDK_INT >= 31 && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestBtPermissionThenRefresh(); setStatus("Grant Bluetooth permission first", false); return
        }
        val a = adapter ?: run { setStatus("Bluetooth not supported", false); return }
        try {
            val devices = a.bondedDevices?.toList().orEmpty()
            if (devices.isEmpty()) { setStatus("Pair ROBO in Android Bluetooth settings first", false); return }
            val names = devices.map { "${it.name ?: "Unknown"}  •  ${it.address}" }.toTypedArray()
            AlertDialog.Builder(this).setTitle("SELECT PAIRED DEVICE").setItems(names) { _, which -> connect(devices[which]) }.setNegativeButton("CANCEL", null).show()
        } catch (_: SecurityException) { setStatus("Bluetooth permission required", false) }
    }

    private fun connect(device: BluetoothDevice) {
        setStatus("Connecting to ${device.name ?: "ROBO"}…", false)
        thread {
            try {
                try { socket?.close() } catch (_: Exception) {}
                val s = device.createRfcommSocketToServiceRecord(sppUuid)
                s.connect()
                socket = s; output = s.outputStream
                startStatusReader(s)
                runOnUiThread {
                    deviceLabel.text = "DEVICE  •  ${device.name ?: "ROBO"}"
                    connectButton.text = "CONNECTED"
                    setStatus("● CONNECTED", true)
                }
            } catch (e: Exception) {
                try { socket?.close() } catch (_: Exception) {}
                socket = null; output = null
                runOnUiThread { setStatus("Connection failed • pair ROBO and retry", false) }
            }
        }
    }

    private fun sendSequence(vararg commands: String) {
        thread {
            try {
                val stream = output ?: throw IllegalStateException("Not connected")
                synchronized(sendLock) {
                    commands.forEachIndexed { index, command ->
                        if (index > 0) Thread.sleep(900)
                        stream.write(command.toByteArray(Charsets.UTF_8))
                        stream.flush()
                    }
                }
                runOnUiThread { setStatus("● CONNECTED  •  GAME COMMAND SENT", true) }
            } catch (_: Exception) {
                runOnUiThread { setStatus("Not connected • connect to ROBO first", false) }
            }
        }
    }

    private fun send(command: String) {
        val bytes = command.toByteArray(Charsets.UTF_8)
        thread {
            try {
                val stream = output ?: throw IllegalStateException("Not connected")
                synchronized(sendLock) { stream.write(bytes); stream.flush() }
                runOnUiThread { setStatus("● CONNECTED  •  SENT: $command", true) }
            } catch (_: Exception) {
                runOnUiThread { setStatus("Not connected • connect to ROBO first", false) }
            }
        }
    }

    private fun startStatusReader(s: BluetoothSocket) {
        readerActive = true
        thread(name = "ROBO-Bluetooth-Status") {
            try {
                val input = s.inputStream
                val buffer = ByteArray(256)
                while (readerActive && s.isConnected) {
                    val count = input.read(buffer)
                    if (count > 0) {
                        val message = String(buffer, 0, count, Charsets.UTF_8)
                            .replace("\r", " ").replace("\n", " ").trim()
                        if (message.isNotEmpty()) runOnUiThread {
                            setStatus("ROBO: $message", true)
                        }
                    }
                }
            } catch (_: Exception) {
                if (readerActive) runOnUiThread { setStatus("Bluetooth connection lost", false) }
            }
        }
    }

    private fun setStatus(message: String, connected: Boolean) {
        if (!::status.isInitialized) return
        status.text = message
        status.setTextColor(if (connected) neon else Color.rgb(255, 100, 120))
    }

    override fun onDestroy() {
        readerActive = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null; output = null
        super.onDestroy()
    }
}
