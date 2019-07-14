import java.util.*
import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JFrame

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val frm = JFrame()
            val gameForm = GameForm()
            frm.contentPane = gameForm.panel1
            frm.isVisible = true
            frm.setSize(500, 410)
            var onButtonClicked = listOf<() -> Unit>()
            val buttons: List<() -> JButton> = listOf({ gameForm.button0 }, { gameForm.button1 }, { gameForm.button2 }, { gameForm.button3 })
            buttons.forEachIndexed { buttonNumber, button ->
                button().addActionListener { onButtonClicked[buttonNumber]() }
            }
            fun showScene(scene: Scene) {
                scene.runOnShow(scene)
                onButtonClicked = scene.sceneButtons.map { { showScene(it.destinationScene()) } }
                buttons.forEach { it().isVisible = false }
                scene.sceneButtons.forEachIndexed { index, s ->
                    buttons[index]().isVisible = true
                    buttons[index]().text = s.buttonText
                }
                gameForm.Label.icon = ImageIcon(scene.SceneImage)
                gameForm.textArea1.text = scene.mainText
                gameForm.textArea2.text = "Level: ${heroStats.level}\nExp: ${heroStats.experience} \nMax Health: ${heroStats.maxHealth}\nCurrent Health: ${heroStats.currentHealth}\nArmour: ${heroStats.armor}\n"
                var result = ""
                heroStats.weapons.forEach { result = result + "\n" + it.wepname }
                gameForm.textArea3.text = "\nInventory: $result"
            }
            showScene(ModeSelectionScene())
        }
    }
}

//object Hero {
    var heroStats = Fightable(
            name = "Our hero",
            weapons = mutableListOf(
                    Weapon("Starter knife", 4, 3, CombatEffect.None()),
                    Weapon("Starter buckler", 1, 9, CombatEffect.Stun(50)),
                    Weapon("groin kick", 2, 2, CombatEffect.None())
            ),
            maxHealth = 10,
            armor = 5,
            level = 1,
            experience = 0,
            expGiven = 0
    )

    lateinit var lastCheckpointStats: Fightable
    var lastCheckpointWand: Boolean = false
    var lastCheckpointScene: Scene = ModeSelectionScene()
    var wand = false
    val checkLevelUp: (() -> Unit, () -> Unit) -> Unit = { runOnLevel, runOnNoLevel ->
        var didLevel = false
        listOf(4, 15, 30, 60).forEachIndexed { index, thresh ->
            if (thresh < heroStats.experience && heroStats.level < index + 2) {
                didLevel = true
                heroStats.level = index + 2
                heroStats.maxHealth += index + 2
            }
        }
        if (didLevel) {
            heroStats.currentHealth = heroStats.maxHealth
            runOnLevel()
        } else runOnNoLevel()
    }
    val hitCheckpoint: (Scene) -> Unit = { scene ->
        lastCheckpointScene = scene
        lastCheckpointStats = heroStats.copy()
        lastCheckpointWand = wand
    }
    val loadCheckpointHero: () -> Unit = {
        heroStats = lastCheckpointStats
        wand = lastCheckpointWand
    }

    fun Resolve(scene: Scene, attacker: Fightable, weapon: Weapon, attacked: Fightable, onMiss: (Scene) -> Unit, onHit: (Scene) -> Unit, onKill: (Scene) -> Unit) {
        val newattackedHealth = attacked.currentHealth - weapon.damage
        val rand = Random().nextInt(100)
        val ail = attacker.ailment
        if (ail is CombatEffect.Stun && ail.chance > rand) {
            onMiss(scene)
        } else if (newattackedHealth < 1) {
            onKill(scene)
        } else {
            attacked.currentHealth = newattackedHealth
            attacked.ailment = weapon.wepType
            onHit(scene)
        }
        if (attacker.ailment is CombatEffect.Stun) attacker.ailment = CombatEffect.None()
    }
//}

class SceneButton(
        var buttonText: String = "default",
        var destinationScene: () -> Scene = { WelcomeScene() }
)

interface Scene {
    var SceneImage: String
    var mainText: String
    var sceneButtons: MutableList<SceneButton>
    var runOnShow: (Scene) -> Unit
}

sealed class CombatEffect {
    class None : CombatEffect()
    class Stun(var chance: Int) : CombatEffect()
}

data class Weapon(var wepname: String, var damage: Int, var speed: Int, var wepType: CombatEffect)

interface Enemy {
    var combatStats: Fightable
    fun healthStatus() =  "${this.combatStats.name} has ${this.combatStats.currentHealth} health"
}

data class Fightable(
        var expGiven: Int,
        var name: String,
        var weapons: List<Weapon>,
        var maxHealth: Int,
        var armor: Int,
        var level: Int = 1,
        var experience: Int = 0,
        var currentHealth: Int = maxHealth,
        var ailment: CombatEffect = CombatEffect.None()
) {


}

class UndeadKnight : Enemy {
    override var combatStats = Fightable(
            name = "Undead Knight",
            weapons = mutableListOf(
                    Weapon(
                            wepname = "Rusty great sword",
                            damage = 2,
                            speed = 2,
                            wepType = CombatEffect.None()
                    )
            ),
            maxHealth = 12,
            armor = 4,
            expGiven = 15
    )
}

class Kobold : Enemy {
    override var combatStats = Fightable(
            name = "kobold",
            weapons = mutableListOf(
                    Weapon(
                            wepname = "kobold dagger",
                            damage = 3,
                            speed = 5,
                            wepType = CombatEffect.None()
                    )
            ),
            maxHealth = 8,
            armor = 3,
            expGiven = 15
    )
}

class Hag : Enemy {
    override var combatStats = Fightable(
            name = "Hag",
            weapons = mutableListOf(
                    Weapon(
                            wepname = "Arcane Blast",
                            damage = 6,
                            speed = 1,
                            wepType = CombatEffect.None()
                    )
            ),
            maxHealth = 8,
            armor = 1,
            expGiven = 15
    )
}

class ModeSelectionScene : Scene {
    override var runOnShow: (Scene) -> Unit = {}
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "Begin the story mode or enter the arena and battle other players!"
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Story Mode",
                    destinationScene = { WelcomeScene() }
            ),
            SceneButton(
                    buttonText = "Multiplayer Arena",
                    destinationScene = { Arena() }
            )
    )
}

class Arena : Scene {
    override var sceneButtons: MutableList<SceneButton> = mutableListOf()
    override var runOnShow: (Scene) -> Unit = {}
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You have entered the multiplayer arena."
}

class WelcomeScene : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You open your eyes and as the world comes into focus you become scared. You've never been to this place and have no memory of getting here. An alien plant bobbing nearby seems to contort and from it a voice emanates.\n\n" +
            "'Welcome, my child. May your stay here be less painful than it is for most.' The voice seems close, but the plant quickly softens and returns to its sunshine languishing.\n\nYou sit in shock for a moment before realising you have only two choices. In the near distance is an excavated bit of earth surrounded by crude palisade wall.\n\n" +
            "You can go check for any signs of civilised life, or you can sit here and waste away."

    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Explore the pit",
                    destinationScene = { CenterPitScene(mode = 1) }
            ),
            SceneButton(
                    buttonText = "Sit in safety",
                    destinationScene = { ScaredScene(WelcomeScene()) }
            )
    )

    override var runOnShow: (Scene) -> Unit = {
        hitCheckpoint(WelcomeScene())
    }
}

class ScaredScene(backScene: Scene) : Scene {
    override var SceneImage: String = "src/elephantman.jpg"
    override var runOnShow: (Scene) -> Unit = {}
    override var mainText = "You sit frozen, unsure of where you are or what to do. After some time, the temporary peace becomes discomfort. Stand, ${heroStats.name}, and by fate be borne on wings of fire!"
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Stand and explore",
                    destinationScene = { backScene }
            )
    )
}

class CenterPitScene(mode: Int) : Scene {
    override var SceneImage: String = "src/elephantman.jpg"
    override var mainText: String = "lalal"
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Fight the beast",
                    destinationScene = {
                        FightScene(
                                enemy = Kobold(),
                                ongoing = false,
                                winScene = SidePitScene()
                        )
                    }
            ),
            SceneButton(
                    buttonText = "Attempt to flee",
                    destinationScene = { FleePit() }
            )
    )
    override var runOnShow: (Scene) -> Unit = {
        if (mode == 1) it.mainText = "Barely more than a bit of cleared earth, the pit shows fresh signs of life. A dying fire. The Burnt bones of a recent meal.\n" +
                "You smell the wretched danger before you see it. A panting Kobold springs into action, appearing in front of you with eyes flashing at the chance for a fight." +
                "Its scarred, stinking body and jagged blade tell you there's no chance for diplomacy."
        if (mode == 2) it.mainText = "Wand in hand, you return to the Pit where the Kobolds reside. Do you wish to try your luck with another beast or will you sit a moment to inspect the wand?"
        if (mode == 3) {
            it.mainText = "You return to the den of the Kobolds empty handed after exploring the ruins for a time. Do you wish to fight a wretched Kobold or return to the ruins?"
            it.sceneButtons[1].buttonText = "Return to the altar"
            it.sceneButtons[1].destinationScene = { FindWandScene() }
        }
        if (wand) {
            it.sceneButtons[1].buttonText = "Draw your wand and examine the inscription"
            it.sceneButtons[1].destinationScene = {
                MagicUpgradeScene(barracks(1))
            }
        }
    }
}

class FleePit : Scene {
    override var SceneImage: String = "src/elephantman.jpg"
    override var runOnShow: (Scene) -> Unit = {}

    override var mainText = "You turn your back on the salivating wretch which seeks your life and move to escape. " +
            "Just as you turn to run, a greedy Kobold waiting for the outcome of the battle steps in your way holding a thin blade.\n" +
            "It slips in under your ribs, piercing your heart."
    override var sceneButtons = mutableListOf(
            SceneButton("Next", { DeathScene() })
    )
}

class GetBuffedScene(backScene: Scene, buff: (Scene) -> Unit) : Scene {
    override var mainText = "you are more buff now"
    override var SceneImage: String = "src/elephantman.jpg"
    override var sceneButtons = mutableListOf(
            SceneButton("Speak again to your magic friend", { MagicUpgradeScene(backScene) })
    )
    override var runOnShow:(Scene)->Unit = {
        buff(it)
    }
}

class MagicUpgradeScene(backScene: Scene) : Scene {
    override var SceneImage: String= "src/elephantman.jpg"
    override var runOnShow: (Scene) -> Unit={}
    override var mainText = "You look closely upon the wand. The tiny etchings begin to glow with a thin silver light, moving across the surface of the wand. Coming from the centre of your belly, you feel an immense pull as if your entire being was drawn through the eye of needle." +
            "You find yourself in the realm of the Wand.\n" +
            "Without words, a humanoid form of pure light speaks to you through the mist. Towering above you, he lumbers slowly but you know, somehow, that he wishes you only kindness. You understand in an instant that he will heal your wounds and fortify your being."
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Accept the Wandman's defensive enchantment",
                    destinationScene = {
                        GetBuffedScene(
                                backScene = backScene,
                                buff = {
                                    if (heroStats.armor < 6) {
                                        heroStats.armor++
                                        it.mainText = "The entity encircles you with a whirling energy. Your skin and muscle harden in an instant. You feel as if your body has been tempered in a smith's fire abd your armour is now ${heroStats.armor}."
                                    } else {
                                        it.mainText = "You have already reaped the benefits of the fortifying enchantment."
                                    }
                                }
                        )
                    }
            ),
            SceneButton(
                    buttonText = "Seek healing magics",
                    destinationScene = {
                        GetBuffedScene(
                                backScene = backScene,
                                buff = {
                                    if (heroStats.currentHealth < heroStats.maxHealth) {
                                        heroStats.currentHealth = heroStats.maxHealth
                                        it.mainText = "You are imbued with vitality. Your health is replenished." +
                                                "Your health is now ${heroStats.currentHealth}."
                                    } else
                                        it.mainText = "You are already completely healed. You return to the wand master."

                                }
                        )
                    }

            ),
            SceneButton("Leave the realm of the Wand", { backScene })
    )
}

class gotoCheckpointscene() : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You will return to your last checkpoint"
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Next",
                    destinationScene = { lastCheckpointScene }
            ))

    override var runOnShow: (Scene) -> Unit = {
        loadCheckpointHero()
    }
}

class DeathScene : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "Your wounds are simply too great. You rest your head on the ground and your consciousness slips away."
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Return to Checkpoint",
                    destinationScene = {
                        gotoCheckpointscene()
                    }
            )
    )
    override var runOnShow: (Scene) -> Unit = {}
}


class FindWandScene : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You have entered the multiplayer arena."
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Return to the Kobold encampment.",
                    destinationScene = {
                        CenterPitScene(mode = 2)
                    }
            )
    )
    override var runOnShow: (Scene) -> Unit = {
        if (wand) {
            it.mainText = "You see the empty altar where the wand once was."
        } else {
            it.mainText = "You approach the burial mound and begin to climb. Sitting upon it is an altar, and upon that in turn an ancient wand. You quickly stash it amongst your cloak."
            wand = true
        }
    }
}

class barracks(mode: Int) : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You have entered the multiplayer arena."
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Search the barracks",
                    destinationScene = {
                        FightScene(
                                enemy = UndeadKnight(),
                                ongoing = false,
                                winScene = barracks(2)
                        )
                    }
            ),
            SceneButton(
                    buttonText = "Use the wand",
                    destinationScene = { MagicUpgradeScene(barracks(3)) }
            )
    )
    override var runOnShow: (Scene) -> Unit = {
        hitCheckpoint(barracks(mode))
        if (mode == 1) it.mainText = "You are thrown out of the Wandman's realm and find yourself standing in front of an ancient barracks. Do you wish to search the building or return to the safety of the Wandman's realm?"
        if (mode == 2) {
            it.mainText = "You stand again before the barracks. Do you wish to challenge an Undead Knight or return to the safety of the Wandman's realm?"
        }
        if (mode == 3) {
            it.mainText = "You are thrown out of the Wandman's realm and find yourself standing again before the barracks. Do you wish to challenge an Undead Knight or return back to the Wandman's realm?"
        }
        if (mode == 2 || mode == 3) {
            it.sceneButtons[0].buttonText = "Fight an Undead Knight"
            it.sceneButtons.add(SceneButton(
                    buttonText = "Leave",
                    destinationScene = { DismalPlain() }
            ))
        }
    }
}

class DismalPlain : Scene {
    override var SceneImage="src/elephantman.jpg"
    override var runOnShow: (Scene) -> Unit={}
    override var mainText = "You leave the rotting barracks and venture into surrounding plains. The soil and sky feel heavy and dirty.\n" +
            "In the near distance, the scream of swamp hags echoes. Do you wish to investigate?"
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Investigate",
                    destinationScene = { HagFight() }
            ),
            SceneButton(
                    buttonText = "Return to Barracks",
                    destinationScene = { barracks(2) }
            )
    )
}

class HagFight : Scene {
    override var runOnShow: (Scene) -> Unit = {}
    override var SceneImage = "src/elephantman.jpg"

    override var mainText = "You creep through the dank foliage and come upon a musty grove foul with the smell of death.\n" +
            "By putrid shores a group of crusty hags, mad with age and arcane power poured blood into a silver contained\n" +
            "One notices you and in an instant is upon you."
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Prepare!",
                    destinationScene = {
                        FightScene(
                                enemy = Hag(),
                                ongoing = false,
                                winScene = DismalPlain()
                        )
                    }
            )
    )
}


class FightScene(enemy: Enemy, ongoing: Boolean, winScene: Scene) : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You have entered the multiplayer arena."

    override var sceneButtons = {
        val result = mutableListOf<SceneButton>()
        heroStats.weapons.forEach { weapon ->
            result.add(
                    SceneButton(
                            buttonText = "use your ${weapon.wepname}",
                            destinationScene = {
                                if (weapon.speed > enemy.combatStats.weapons[0].speed) {
                                    HitEnemyScene(enemy, GetHitScene(enemy, FightScene(enemy, true, winScene)), winScene, weapon)
                                } else {
                                    GetHitScene(enemy, HitEnemyScene(enemy, FightScene(enemy, true, winScene), winScene, weapon))
                                }
                            }
                    )
            )
        }
        result
    }()
    override var runOnShow: (Scene) -> Unit = {
        if (ongoing) {
            it.mainText = "you continue ur battle with ${enemy.combatStats.name}. ${enemy.healthStatus()}."
        } else {
            it.mainText = "A ${enemy.combatStats.name} appears! ${enemy.healthStatus()}."
        }
    }
}

class HitEnemyScene(enemy: Enemy, responseScene: Scene, winScene: Scene, weapon: Weapon) : Scene {
    override var sceneButtons: MutableList<SceneButton> = mutableListOf(
            SceneButton()
    )
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "unused"
    override var runOnShow: (Scene) -> Unit = { theScene ->
        Resolve(
                scene = theScene,
                attacker = heroStats,
                weapon = weapon,
                attacked = enemy.combatStats,
                onHit = { scene ->
                    scene.mainText = "You hit the ${enemy.combatStats.name} for ${weapon.damage} damage"
                    scene.sceneButtons[0].destinationScene = { responseScene }
                    scene.sceneButtons[0].buttonText = "take that!"
                },
                onKill = { scene ->
                    heroStats.experience += enemy.combatStats.expGiven
                    checkLevelUp(
                            {
                                scene.mainText = "You struck down the ${enemy.combatStats.name} and level up! You now are level ${heroStats.level}!."
                            },
                            {
                                scene.mainText = "You struck down the ${enemy.combatStats.name}! You now have ${heroStats.experience} experience points!."
                            }
                    )
                    scene.sceneButtons[0].destinationScene = { winScene }
                    scene.sceneButtons[0].buttonText = "awesome"
                },
                onMiss = { scene ->
                    scene.mainText = "${heroStats.name} is stunned and missed their turn!"
                    scene.sceneButtons[0].buttonText = "damn"
                    scene.sceneButtons[0].destinationScene = { responseScene }
                }
        )
    }
}

class GetHitScene(enemy: Enemy, responseScene: Scene) : Scene {
    override var sceneButtons: MutableList<SceneButton> = mutableListOf(SceneButton())
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "You have entered the multiplayer arena."
    override var runOnShow: (Scene) -> Unit = { theScene ->
        Resolve(
                scene = theScene,
                attacker = enemy.combatStats,
                weapon = enemy.combatStats.weapons.first(),
                attacked = heroStats,
                onHit = { scene ->
                    scene.mainText = "The ${enemy.combatStats.name} hits you for ${enemy.combatStats.weapons[0].damage} damage!"
                    scene.sceneButtons[0].destinationScene = { responseScene }
                    scene.sceneButtons[0].buttonText = "I can handle it"
                },
                onKill = { scene ->
                    scene.mainText = "The ${enemy.combatStats.name} hits you a fatal blow."
                    scene.sceneButtons[0].destinationScene = { DeathScene() }
                    scene.sceneButtons[0].buttonText = "Next"
                },
                onMiss = { scene ->
                    scene.mainText = "the ${enemy.combatStats.name} is stunned and missed their turn!"
                    scene.sceneButtons[0].buttonText = "cool"
                    scene.sceneButtons[0].destinationScene = { responseScene }
                }
        )
    }
}

class SidePitScene() : Scene {
    override var SceneImage = "src/elephantman.jpg"
    override var mainText = "Beyond the pit of Kobolds is an old burial mound, and upon it lies an altar."
    override var sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Return to the Kobolds pit",
                    destinationScene = { CenterPitScene(mode = 3) }
            ),
            SceneButton(
                    buttonText = "Climb the mound and approach the altar.",
                    destinationScene = { FindWandScene() }
            )
    )
    override var runOnShow: (Scene) -> Unit = { thescene ->

    }

}



