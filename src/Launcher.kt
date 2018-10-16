import java.util.*
import javax.swing.JButton
import javax.swing.JFrame

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val frm = JFrame()
            frm.contentPane = UserInterface.gameForm.panel1
            frm.isVisible = true
            frm.setSize(500,500)
            UserInterface.setupButtonListeners()
            UserInterface.showScene{WelcomeScene()}
        }
    }
}

object UserInterface {
    val gameForm = GameForm()
    var currentSceneButtons = listOf<SceneButton>()
    val buttons:List<()->JButton> = listOf({UserInterface.gameForm.button0}, {UserInterface.gameForm.button1}, {UserInterface.gameForm.button2},{UserInterface.gameForm.button3})
    val setupButtonListeners:()->Unit = {
        buttons.forEachIndexed { buttonNumber, button ->
            button().addActionListener{showScene(currentSceneButtons[buttonNumber].destinationScene)}
        }
    }
    val showScene:(()->Scene)->Unit = { theScene->
        val ascene = theScene()
        ascene.runOnShow(ascene)
        currentSceneButtons = ascene.sceneButtons
        buttons.forEach{it().isVisible = false}
        ascene.sceneButtons.forEachIndexed { index, s ->
            buttons[index]().isVisible = true
            buttons[index]().text = s.buttonText
        }
        UserInterface.gameForm.textArea1.text = ascene.mainText
        UserInterface.gameForm.textArea2.text= "Level: ${Hero.heroLevel}\nExp: ${Hero.heroExp} \nMax Health: ${Hero.heroFightable.maxHealth}\nCurrent Health: ${Hero.heroFightable.currentHealth}\nArmour: ${Hero.heroFightable.armor}\n"
    }
}

object Hero {
    var heroFightable = Fightable(
            name = "Our hero",
            mainWeapon =  Weapon("Starter knife",4,3,CombatEffect.None()),
            maxHealth = 10,
            armor = 5
    )
    var heroLevel = 1
    var heroExp = 0
    var lastCheckpointHero:Hero = this
    var lastCheckpointScene:Scene = WelcomeScene()
    var wand = false
    var leftHand:Weapon = Weapon("Starter buckler",1,9,CombatEffect.Stun(50))
    val checkLevelUp:(()->Unit, ()->Unit)->Unit = { runOnLevel, runOnNoLevel->
        var didLevel = false
        listOf(4,15,30,60).forEachIndexed {index,thresh->
            if(thresh < heroExp && heroLevel<index+2){
                didLevel = true
                Hero.heroLevel = index+2
                Hero.heroFightable.maxHealth += index+2
            }
        }
        if(didLevel){
            Hero.heroFightable.currentHealth = Hero.heroFightable.maxHealth
            runOnLevel()
        } else runOnNoLevel()
    }
    val hitCheckpoint:(Scene)->Unit={scene->
        lastCheckpointScene = scene
        lastCheckpointHero = this
    }
    val loadCheckpointHero:()->Unit={
        heroExp = lastCheckpointHero.heroExp
        heroFightable.currentHealth = lastCheckpointHero.heroFightable.currentHealth
        heroFightable.armor = lastCheckpointHero.heroFightable.armor
        heroFightable.maxHealth = lastCheckpointHero.heroFightable.maxHealth
        heroLevel = lastCheckpointHero.heroLevel
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

open class Enemy(var eFightable:Fightable, var expGiven: Int)

open class Fightable(
        var name:String,
        var mainWeapon: Weapon,
        var maxHealth:Int,
        var armor:Int) {
    var currentHealth:Int = maxHealth
    var ailment:CombatEffect = CombatEffect.None()
    val healthStatus = {"${this.name} has ${this.currentHealth} currentHealth"}
}

class UndeadKnight:Enemy(
        eFightable = Fightable(
        name = "Undead Knight",
        mainWeapon =  Weapon(
                wepname = "Rusty great sword",
                damage = 2,
                speed =  2,
                wepType =  CombatEffect.None()),
        maxHealth =  12,
        armor = 4),
        expGiven = 15
)

class Kobold:Enemy(
        eFightable = Fightable(
        name = "kobold",
        mainWeapon = Weapon(
                wepname = "kobold dagger",
                damage = 3,
                speed = 5,
                wepType = CombatEffect.None()
        ),
        maxHealth = 8,
        armor = 3),
        expGiven = 16
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
        mainText = "You sit frozen, unsure of where you are or what to do. After some time, the temporary peace becomes discomfort. Stand, ${Hero.heroFightable.name}, and by fate be borne on wings of fire!",
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
                                        if(Hero.heroFightable.armor < 6) {
                                            Hero.heroFightable.armor++
                                            it.mainText = "The entity encircles you with a whirling energy. Your skin and muscle harden in an instant. You feel as if your body has been tempered in a smith's fire abd your armour is now ${Hero.heroFightable.armor}."
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
                                        if(Hero.heroFightable.currentHealth < Hero.heroFightable.maxHealth){
                                            Hero.heroFightable.currentHealth = Hero.heroFightable.maxHealth
                                            it.mainText =  "You are imbued with vitality. Your health is replenished." +
                                                    "Your health is now ${Hero.heroFightable.currentHealth}."
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
                                    mainText = "You awake to find your wounds healed, but your items are lost and your magical benefits removed.\n" +
                                            "You will return to your last checkpoint",
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
            Hero.hitCheckpoint(FindWandScene())
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
                it.sceneButtons[0].buttonText = "Fight an Undead Knight"
            }
            if(mode==3){
                it.mainText ="You are thrown out of the Wandman's realm and find yourself standing again before the barracks. Do you wish to challenge an Undead Knight or return back to the Wandman's realm?"
                it.sceneButtons[0].buttonText ="Fight an Undead Knight"
            }
        }
)


class FightScene(enemy:Enemy,ongoing:Boolean,winScene:Scene): Scene(
        mainText = "A ${enemy.eFightable.name} appears! ${enemy.eFightable.healthStatus()}. ${Hero.heroFightable.healthStatus()}",
        sceneButtons = mutableListOf(
                SceneButton(
                        buttonText = "use your ${Hero.leftHand.wepname}",
                        destinationScene = {
                            if(Hero.leftHand.speed>enemy.eFightable.mainWeapon.speed){
                                HitEnemyScene(enemy,GetHitScene(enemy.eFightable,FightScene(enemy,true,winScene)),winScene,Hero.leftHand)
                            }else{
                                GetHitScene(enemy.eFightable,HitEnemyScene(enemy,FightScene(enemy,true,winScene),winScene,Hero.leftHand))
                            }
                        }
                ),
                SceneButton(
                        buttonText = "use your ${Hero.heroFightable.mainWeapon.wepname}",
                        destinationScene = {
                            if(Hero.heroFightable.mainWeapon.speed>enemy.eFightable.mainWeapon.speed){
                                HitEnemyScene(enemy,GetHitScene(enemy.eFightable,FightScene(enemy,true,winScene)),winScene,Hero.heroFightable.mainWeapon)
                            }else{
                                GetHitScene(enemy.eFightable, HitEnemyScene(enemy,  FightScene(enemy, true, winScene) , winScene, Hero.heroFightable.mainWeapon))
                            }
                        }
                )
        ),
        runOnShow = {
            if(ongoing){
                it.mainText = "you continue ur battle with ${enemy.eFightable.name}. ${enemy.eFightable.healthStatus()}. ${Hero.heroFightable.healthStatus()}"
            }
        }
)

class HitEnemyScene(enemy: Enemy,responseScene:Scene,winScene: Scene,weapon: Weapon): Scene(
        runOnShow = {
            val newEnemyHealth = enemy.eFightable.currentHealth - weapon.damage
            val rand = Random().nextInt(100)
            val ail = Hero.heroFightable.ailment
            if(ail is CombatEffect.Stun && ail.chance>rand){
                it.mainText = "${Hero.heroFightable.name} is stunned and missed their turn!"
                it.sceneButtons[0].buttonText = "damn"
                it.sceneButtons[0].destinationScene = {responseScene}
            }else if(newEnemyHealth<1){
                Hero.heroExp += enemy.expGiven
                Hero.checkLevelUp(
                        {it.mainText = "You struck down the ${enemy.eFightable.name} and level up! You now are level ${Hero.heroLevel}!."},
                        {it.mainText = "You struck down the ${enemy.eFightable.name}! You now have ${Hero.heroExp} experience points!."}
                )
                it.sceneButtons[0].destinationScene = {winScene}
                it.sceneButtons[0].buttonText = "awesome"
            }else{
                enemy.eFightable.currentHealth = newEnemyHealth
                enemy.eFightable.ailment = weapon.wepType
                it.mainText = "You hit the ${enemy.eFightable.name} for ${weapon.damage} damage"
                it.sceneButtons[0].destinationScene = {responseScene}
                it.sceneButtons[0].buttonText = "take that!"
            }
            if(Hero.heroFightable.ailment is CombatEffect.Stun) Hero.heroFightable.ailment = CombatEffect.None()
        }
)

class GetHitScene(enemy: Fightable,responseScene: Scene):Scene(
        runOnShow ={
            val newhealth = Hero.heroFightable.currentHealth - enemy.mainWeapon.damage
            val rand = Random().nextInt(100)
            val ail = enemy.ailment
            if(ail is CombatEffect.Stun && ail.chance>rand){
                it.mainText = "the ${enemy.name} is stunned and missed their turn!"
                it.sceneButtons[0].buttonText = "cool"
                it.sceneButtons[0].destinationScene = {responseScene}
            }else if(newhealth<1){
                it.mainText = "The ${enemy.name} hits you a fatal blow."
                it.sceneButtons[0].destinationScene = { DeathScene() }
                it.sceneButtons[0].buttonText = "Next"
            }else{
                Hero.heroFightable.currentHealth = newhealth
                Hero.heroFightable.ailment = enemy.mainWeapon.wepType
                it.mainText = "The ${enemy.name} hits you for ${enemy.mainWeapon.damage} damage!"
                it.sceneButtons[0].destinationScene = {responseScene}
                it.sceneButtons[0].buttonText = "I can handle it"
            }
            if(enemy.ailment is CombatEffect.Stun) enemy.ailment = CombatEffect.None()
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