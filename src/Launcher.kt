import java.util.*
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
            frm.setSize(500,500)
            var onButtonClicked = listOf<()->Unit>()
            val buttons:List<()->JButton> = listOf({gameForm.button0}, {gameForm.button1}, {gameForm.button2},{gameForm.button3})
            buttons.forEachIndexed { buttonNumber, button ->
                button().addActionListener { onButtonClicked[buttonNumber]()}
            }
            fun showScene(scene: Scene){
                scene.runOnShow(scene)
                onButtonClicked = scene.sceneButtons.map { {showScene( it.destinationScene() )}}
                buttons.forEach{it().isVisible = false}
                scene.sceneButtons.forEachIndexed { index, s ->
                    buttons[index]().isVisible = true
                    buttons[index]().text = s.buttonText
                }
                gameForm.textArea1.text = scene.mainText
                gameForm.textArea2.text= "Level: ${Hero.combatStats.level}\nExp: ${Hero.combatStats.experience} \nMax Health: ${Hero.combatStats.maxHealth}\nCurrent Health: ${Hero.combatStats.currentHealth}\nArmour: ${Hero.combatStats.armor}\n"
            }
            showScene(WelcomeScene())
        }
    }
}

object Hero {
    var combatStats = Fightable(
            name = "Our hero",
            weapons =  mutableListOf(
                    Weapon("Starter knife",4,3,CombatEffect.None()),
                    Weapon("Starter buckler",1,9,CombatEffect.Stun(50)),
                    Weapon("groin kick",2,2,CombatEffect.None())
            ),
            maxHealth = 10,
            armor = 5,
            level = 1,
            experience = 0
    )

    lateinit var lastCheckpointStats:Fightable
    var lastCheckpointWand:Boolean = false
    var lastCheckpointScene:Scene = WelcomeScene()
    var wand = false
    val checkLevelUp:(()->Unit, ()->Unit)->Unit = { runOnLevel, runOnNoLevel->
        var didLevel = false
        listOf(4,15,30,60).forEachIndexed {index,thresh->
            if(thresh < combatStats.experience && combatStats.level<index+2){
                didLevel = true
                Hero.combatStats.level = index+2
                Hero.combatStats.maxHealth += index+2
            }
        }
        if(didLevel){
            Hero.combatStats.currentHealth = Hero.combatStats.maxHealth
            runOnLevel()
        } else runOnNoLevel()
    }
    val hitCheckpoint:(Scene)->Unit={scene->
        lastCheckpointScene = scene
        lastCheckpointStats = combatStats.copy()
        lastCheckpointWand = wand
    }
    val loadCheckpointHero:()->Unit={
        combatStats = lastCheckpointStats
        wand = lastCheckpointWand
    }
    fun Resolve(scene:Scene,attacker:Fightable,weapon: Weapon,attacked:Fightable,onMiss:(Scene)->Unit,onHit:(Scene)->Unit,onKill:(Scene)->Unit){
        val newattackedHealth = attacked.currentHealth - weapon.damage
        val rand = Random().nextInt(100)
        val ail = attacker.ailment
        if(ail is CombatEffect.Stun && ail.chance>rand){
            onMiss(scene)
        }else if(newattackedHealth<1){
            onKill(scene)
        }else{
            attacked.currentHealth = newattackedHealth
            attacked.ailment = weapon.wepType
            onHit(scene)
        }
        if(attacker.ailment is CombatEffect.Stun) attacker.ailment = CombatEffect.None()
    }
}

class SceneButton(
        var buttonText:String = "",
        var destinationScene:()->Scene = { Scene() }
)

open class Scene(
        var mainText:String = "",
        var sceneButtons: MutableList<SceneButton> = mutableListOf(SceneButton()),
        var runOnShow:(Scene)->Unit = {}
)

sealed class CombatEffect { class None:CombatEffect() class Stun(var chance:Int):CombatEffect() }

class Weapon(var wepname:String,var damage:Int,var speed:Int,var wepType:CombatEffect)

open class Enemy(
        var combatStats:Fightable,
        var expGiven: Int) {
    val healthStatus = {"${this.combatStats.name} has ${this.combatStats.currentHealth} health"}
}

data class Fightable(
        var name:String,
        var weapons: List<Weapon>,
        var maxHealth:Int,
        var armor:Int,
        var level:Int = 1,
        var experience:Int = 0) {
    var currentHealth:Int = maxHealth
    var ailment:CombatEffect = CombatEffect.None()
}

class UndeadKnight:Enemy(
        combatStats = Fightable(
            name = "Undead Knight",
            weapons =  mutableListOf(
                    Weapon(
                            wepname = "Rusty great sword",
                            damage = 2,
                            speed =  2,
                            wepType =  CombatEffect.None()
                    )
            ),
            maxHealth =  12,
            armor = 4),
        expGiven = 15
)

class Kobold:Enemy(
        combatStats = Fightable(
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
                armor = 3
        ),
        expGiven = 5
)

class Hag:Enemy(
        combatStats = Fightable(
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
                armor = 1
        ),
        expGiven = 15
)

class WelcomeScene:Scene(
        mainText =  "You open your eyes and as the world comes into focus you become scared. You've never been to this place and have no memory of getting here. An alien plant bobbing nearby seems to contort and from it a voice emanates.\n" +
                "'Welcome, my child. May your stay here be less painful than it is for most.' The voice seems close, but the plant quickly softens and returns to its sunshine languishing.\nYou sit in shock for a moment before realising you have only two choices. In the near distance is an excavated bit of earth surrounded by crude palisade wall.\n" +
                "You can go check for any signs of civilised life, or you can sit here and waste away.",

        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText =  "Explore the pit",
                        destinationScene = { CenterPitScene(mode = 1) }
                ),
                SceneButton(
                        buttonText =  "Sit in safety",
                        destinationScene = { ScaredScene(WelcomeScene()) }
                )
        )
        ,
        runOnShow = {
            Hero.hitCheckpoint(WelcomeScene())
        }
)
class ScaredScene(backScene:Scene):Scene(
        mainText = "You sit frozen, unsure of where you are or what to do. After some time, the temporary peace becomes discomfort. Stand, ${Hero.combatStats.name}, and by fate be borne on wings of fire!",
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText =  "Stand and explore",
                        destinationScene = { backScene }
                )
        )
)

class CenterPitScene(mode:Int):Scene(
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText =  "Fight the beast",
                        destinationScene = {
                            FightScene(
                                    enemy = Kobold(),
                                    ongoing = false,
                                    winScene = SidePitScene()
                            )
                        }
                ),
                SceneButton(
                        buttonText = "Attempt to flee" ,
                        destinationScene = { FleePit() }
                )
        ),
        runOnShow = {
            if(mode==1) it.mainText = "Barely more than a bit of cleared earth, the pit shows fresh signs of life. A dying fire. The Burnt bones of a recent meal.\n" +
                    "You smell the wretched danger before you see it. A panting Kobold springs into action, appearing in front of you with eyes flashing at the chance for a fight." +
                    "Its scarred, stinking body and jagged blade tell you there's no chance for diplomacy."
            if(mode==2) it.mainText = "Wand in hand, you return to the Pit where the Kobolds reside. Do you wish to try your luck with another beast or will you sit a moment to inspect the wand?"
            if(mode==3){
                it.mainText = "You return to the den of the Kobolds empty handed after exploring the ruins for a time. Do you wish to fight a wretched Kobold or return to the ruins?"
                it.sceneButtons[1].buttonText = "Return to the altar"
                it.sceneButtons[1].destinationScene = {FindWandScene()}
            }
            if(Hero.wand){
                it.sceneButtons[1].buttonText = "Draw your wand and examine the inscription"
                it.sceneButtons[1].destinationScene = {
                    MagicUpgradeScene(barracks(1))
                }
            }
        }
)

class FleePit:Scene(
        mainText = "You turn your back on the salivating wretch which seeks your life and move to escape. " +
                "Just as you turn to run, a greedy Kobold waiting for the outcome of the battle steps in your way holding a thin blade.\n" +
                "It slips in under your ribs, piercing your heart.",
        sceneButtons = mutableListOf(
                SceneButton("Next",{DeathScene()})
        )
)

class GetBuffedScene(backScene: Scene,buff:(Scene)->Unit):Scene(
        sceneButtons = mutableListOf(
                SceneButton("Speak again to your magic friend",{ MagicUpgradeScene(backScene) })
        ),
        runOnShow = {
            buff(it)
        }
)

class MagicUpgradeScene(backScene: Scene):Scene(
        mainText =  "You look closely upon the wand. The tiny etchings begin to glow with a thin silver light, moving across the surface of the wand. Coming from the centre of your belly, you feel an immense pull as if your entire being was drawn through the eye of needle." +
                "You find yourself in the realm of the Wand.\n" +
                "Without words, a humanoid form of pure light speaks to you through the mist. Towering above you, he lumbers slowly but you know, somehow, that he wishes you only kindness. You understand in an instant that he will heal your wounds and fortify your being.",
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText =  "Accept the Wandman's defensive enchantment",
                        destinationScene = {
                            GetBuffedScene(
                                    backScene =  backScene,
                                    buff = {
                                        if(Hero.combatStats.armor < 6) {
                                            Hero.combatStats.armor++
                                            it.mainText = "The entity encircles you with a whirling energy. Your skin and muscle harden in an instant. You feel as if your body has been tempered in a smith's fire abd your armour is now ${Hero.combatStats.armor}."
                                        }
                                        else {
                                            it.mainText = "You have already reaped the benefits of the fortifying enchantment."
                                        }
                                    }
                            )
                        }
                ),
                SceneButton(
                        buttonText = "Seek healing magics",
                        destinationScene =  {
                            GetBuffedScene(
                                    backScene = backScene,
                                    buff = {
                                        if(Hero.combatStats.currentHealth < Hero.combatStats.maxHealth){
                                            Hero.combatStats.currentHealth = Hero.combatStats.maxHealth
                                            it.mainText =  "You are imbued with vitality. Your health is replenished." +
                                                    "Your health is now ${Hero.combatStats.currentHealth}."
                                        } else
                                            it.mainText = "You are already completely healed. You return to the wand master."

                                    }
                            )
                        }

                ),
                SceneButton("Leave the realm of the Wand",{ backScene })
        )
)

class DeathScene:Scene(
        mainText =  "Your wounds are simply too great. You rest your head on the ground and your consciousness slips away.",
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText = "Return to Checkpoint",
                        destinationScene = {
                            object :Scene(
                                    mainText = "You will return to your last checkpoint",
                                    sceneButtons = mutableListOf(
                                            SceneButton(
                                                    buttonText = "Next",
                                                    destinationScene = { Hero.lastCheckpointScene }
                                            )
                                    ),
                                    runOnShow = {
                                        Hero.loadCheckpointHero()
                                    }
                            ){}
                        }
                )
        )
)

class FindWandScene:Scene(
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText = "Return to the Kobold encampment.",
                        destinationScene = {
                            CenterPitScene(mode = 2)
                        }
                )
        ),
        runOnShow = {
            if(Hero.wand){
                it.mainText = "You see the empty altar where the wand once was."
            }else{
                it.mainText = "You approach the burial mound and begin to climb. Sitting upon it is an altar, and upon that in turn an ancient wand. You quickly stash it amongst your cloak."
                Hero.wand = true
            }
        }
)

class barracks(mode:Int): Scene(
        sceneButtons = mutableListOf(
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
        ),
        runOnShow = {
            Hero.hitCheckpoint(barracks(mode))
            if(mode==1) it.mainText ="You are thrown out of the Wandman's realm and find yourself standing in front of an ancient barracks. Do you wish to search the building or return to the safety of the Wandman's realm?"
            if(mode==2) {
                it.mainText ="You stand again before the barracks. Do you wish to challenge an Undead Knight or return to the safety of the Wandman's realm?"
            }
            if(mode==3){
                it.mainText ="You are thrown out of the Wandman's realm and find yourself standing again before the barracks. Do you wish to challenge an Undead Knight or return back to the Wandman's realm?"
            }
            if(mode==2||mode==3) {
                        it.sceneButtons[0].buttonText ="Fight an Undead Knight"
                                it.sceneButtons.add(SceneButton(
                                buttonText = "Leave",
                                destinationScene = { DismalPlain() }
                                ))
            }
        }
)
class DismalPlain : Scene(
        mainText = "You leave the rotting barracks and venture into surrounding plains. The soil and sky feel heavy and dirty.\n" +
                "In the near distance, the scream of swamp hags echoes. Do you wish to investigate?",
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText = "Investigate",
                        destinationScene = { HagFight() }
                ),
                SceneButton(
                        buttonText = "Return to Barracks",
                        destinationScene = { barracks(2) }
                )
        )
)

class HagFight : Scene(
        mainText = "You creep through the dank foliage and come upon a musty grove foul with the smell of death.\n" +
"By putrid shores a group of crusty hags, mad with age and arcane power poured blood into a silver contained\n" +
"One notices you and in an instant is upon you.",
        sceneButtons = mutableListOf(
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
)



class FightScene(enemy:Enemy,ongoing:Boolean,winScene:Scene): Scene(
        sceneButtons = {
            val result = mutableListOf<SceneButton>()
            Hero.combatStats.weapons.forEach { weapon ->
                result.add(
                        SceneButton(
                                buttonText = "use your ${weapon.wepname}",
                                destinationScene = {
                                    if(weapon.speed>enemy.combatStats.weapons[0].speed){
                                        HitEnemyScene(enemy,GetHitScene(enemy,FightScene(enemy,true,winScene)),winScene,weapon)
                                    }else{
                                        GetHitScene(enemy,HitEnemyScene(enemy,FightScene(enemy,true,winScene),winScene,weapon))
                                    }
                                }
                        )
                )
            }
            result
        }(),
        runOnShow = {
            if(ongoing){
                it.mainText = "you continue ur battle with ${enemy.combatStats.name}. ${enemy.healthStatus()}."
            }else{
                it.mainText = "A ${enemy.combatStats.name} appears! ${enemy.healthStatus()}."
            }
        }
)

class HitEnemyScene(enemy:Enemy,responseScene:Scene,winScene: Scene,weapon: Weapon): Scene(
        runOnShow = { theScene->
            Hero.Resolve(
                    scene = theScene,
                    attacker = Hero.combatStats,
                    weapon = weapon,
                    attacked = enemy.combatStats,
                    onHit = { scene->
                        scene.mainText = "You hit the ${enemy.combatStats.name} for ${weapon.damage} damage"
                        scene.sceneButtons[0].destinationScene = {responseScene}
                        scene.sceneButtons[0].buttonText = "take that!"
                    },
                    onKill = { scene ->
                        Hero.combatStats.experience += enemy.expGiven
                        Hero.checkLevelUp(
                                {scene.mainText = "You struck down the ${enemy.combatStats.name} and level up! You now are level ${Hero.combatStats.level}!."},
                                {scene.mainText = "You struck down the ${enemy.combatStats.name}! You now have ${Hero.combatStats.experience} experience points!."}
                        )
                        scene.sceneButtons[0].destinationScene = {winScene}
                        scene.sceneButtons[0].buttonText = "awesome"
                    },
                    onMiss = { scene->
                        scene.mainText = "${Hero.combatStats.name} is stunned and missed their turn!"
                        scene.sceneButtons[0].buttonText = "damn"
                        scene.sceneButtons[0].destinationScene = {responseScene}
                    }
            )
        }
)

class GetHitScene(enemy:Enemy,responseScene:Scene):Scene(
        runOnShow ={
            theScene->
            Hero.Resolve(
                    scene = theScene,
                    attacker = enemy.combatStats,
                    weapon = enemy.combatStats.weapons.first(),
                    attacked = Hero.combatStats,
                    onHit = { scene->
                        scene.mainText = "The ${enemy.combatStats.name} hits you for ${enemy.combatStats.weapons[0].damage} damage!"
                        scene.sceneButtons[0].destinationScene = {responseScene}
                        scene.sceneButtons[0].buttonText = "I can handle it"
                    },
                    onKill = { scene ->
                        scene.mainText = "The ${enemy.combatStats.name} hits you a fatal blow."
                        scene.sceneButtons[0].destinationScene = { DeathScene() }
                        scene.sceneButtons[0].buttonText = "Next"
                    },
                    onMiss = { scene->
                        scene.mainText = "the ${enemy.combatStats.name} is stunned and missed their turn!"
                        scene.sceneButtons[0].buttonText = "cool"
                        scene.sceneButtons[0].destinationScene = {responseScene}
                    }
            )
        }
)

class SidePitScene:Scene(
        mainText = "Beyond the pit of Kobolds is an old burial mound, and upon it lies an altar.",
        sceneButtons = mutableListOf(
            SceneButton(
                    buttonText = "Return to the Kobolds pit",
                    destinationScene = { CenterPitScene(mode = 3) }
            ),
            SceneButton(
                    buttonText = "Climb the mound and approach the altar.",
                    destinationScene = { FindWandScene() }
            )
        )
)