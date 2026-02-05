package DrDan.AnimalsGrow.command

import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Vector3d
import DrDan.AnimalsGrow.test.TestRunner

/**
 * Command to run in-game tests for AnimalsGrow plugin.
 * Usage: /agtest (works from console or in-game)
 * 
 * Outputs structured log messages for parsing by test.sh:
 *   [AG_TEST:START]
 *   [AG_TEST:testName:PASS]
 *   [AG_TEST:testName:FAIL:reason]
 *   [AG_TEST:END:passed/total]
 */
class AnimalsGrowTestCommand : AbstractWorldCommand {
    constructor() : super("agtest", "Run AnimalsGrow plugin tests")

    override fun execute(
        commandContext: CommandContext,
        world: World,
        store: Store<EntityStore>
    ) {
        println("=== AnimalsGrow Test Suite ===")        
        TestRunner.runAllTests(world, store)
    }

    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }
}
