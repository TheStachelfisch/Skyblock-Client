package skyblockclient.features.dungeons

import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import skyblockclient.SkyblockClient.Companion.config
import skyblockclient.SkyblockClient.Companion.mc
import skyblockclient.events.ClickEvent
import skyblockclient.utils.Utils.isFloor
import skyblockclient.utils.Utils.rightClick
import java.util.*

class ArrowAlign {

    private val area = BlockPos.getAllInBox(BlockPos(197, 125, 278), BlockPos(197, 121, 274))
            .toList().sortedWith { a, b ->
            if (a.y == b.y) return@sortedWith b.z - a.z
            if (a.y < b.y) return@sortedWith 1
            if (a.y > b.y) return@sortedWith -1
            return@sortedWith 0
        }
    private val neededRotations = HashMap<Pair<Int, Int>, Int>()
    private var ticks = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || !config.arrowAlign && !config.autoCompleteArrowAlign || !isFloor(7)) return
        if (ticks % 20 == 0) {
            if (mc.thePlayer.getDistanceSq(BlockPos(197, 122, 276)) <= 20 * 20) calculate()
            ticks = 0
        }
        ticks++
    }

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.RightClickEvent) {
        if (!config.arrowAlign && !config.autoCompleteArrowAlign || !isFloor(7) || mc.objectMouseOver == null) return
        if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            if (mc.objectMouseOver.entityHit is EntityItemFrame) {
                if (mc.thePlayer.isSneaking && config.arrowAlignSneakOverride) return
                val frame = mc.objectMouseOver.entityHit as EntityItemFrame
                val x = 278 - frame.hangingPosition.z
                val y = 124 - frame.hangingPosition.y
                if (x in 0..4 && y in 0..4) {
                    val clicks = neededRotations[Pair(x, y)] ?: return
                    if (clicks == 0) {
                        event.isCanceled = true
                        return
                    }
                    neededRotations[Pair(x, y)] = clicks - 1
                    if (config.autoCompleteArrowAlign && clicks > 1) {
                        rightClick()
                    }
                }
            }
        }
    }

    private fun calculate() {
        val frames = mc.theWorld.getEntities(EntityItemFrame::class.java) {
            it != null && area.contains(it.position) && it.displayedItem != null
        }
        if (frames.isNotEmpty()) {
            val solutions = HashMap<Pair<Int, Int>, Int>()
            val maze = Array(5) { IntArray(5) }
            val queue = LinkedList<Pair<Int, Int>>()
            val visited = Array(5) { BooleanArray(5) }
            neededRotations.clear()
            area.withIndex().forEach { (i, pos) ->
                val x = i % 5
                val y = i / 5
                val frame = frames.find { it.position == pos } ?: return@forEach
                // 0 = null, 1 = arrow, 2 = end, 3 = start
                maze[x][y] = when (frame.displayedItem.item) {
                    Items.arrow -> 1
                    Item.getItemFromBlock(Blocks.wool) -> {
                        when (frame.displayedItem.itemDamage) {
                            5 -> 3
                            14 -> 2
                            else -> 0
                        }
                    }
                    else -> 0
                }
                when (maze[x][y]) {
                    1 -> neededRotations[Pair(x, y)] = frame.rotation
                    3 -> queue.add(Pair(x, y))
                }
            }
            while (queue.size != 0) {
                val s = queue.poll()
                val directions = arrayOf(intArrayOf(1, 0), intArrayOf(0, 1), intArrayOf(-1, 0), intArrayOf(0, -1))
                (3 downTo 0).forEach {
                    val x = (s.first + directions[it][0])
                    val y = (s.second + directions[it][1])
                    if (x in 0..4 && y in 0..4) {
                        val rotations = it * 2 + 1
                        if (solutions[Pair(x, y)] == null && maze[x][y] in 1..2) {
                            queue.add(Pair(x, y))
                            solutions[s] = rotations
                            if (!visited[s.first][s.second]) {
                                var neededRotation = neededRotations[s] ?: return@forEach
                                neededRotation = rotations - neededRotation
                                if (neededRotation < 0) neededRotation += 8
                                neededRotations[s] = neededRotation
                                visited[s.first][s.second] = true
                            }
                        }
                    }
                }
            }
        }
    }
}
