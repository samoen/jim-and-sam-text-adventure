import java.util.*
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

open class Scene(
        var mainText:String = "",
        var button1Text:String = "",
        var button2Text:String = "",
        var button3Text:String = "",
        var nextScene1:()->Scene = {Scene()},
        var nextScene2:()->Scene = {Scene()},
        var nextScene3:()->Scene = {Scene()},
        var numberOfButtons:Int = 2,
        var runOnShow:(Scene)->Unit = {}
)

class UndeadKnight:Enemy(
        name = "Undead Knight",
        mainWeapon =  Weapon(
                wepname = "Rusty great sword",
                damage = 2,
                speed =  2,
                wepType =  CombatEffect.None()),
        maxHealth =  12,
        armor = 4,
        expGiven = 15
)

class Kobold:Enemy(
        name = "kobold",
        mainWeapon = Weapon(
                wepname = "kobold dagger",
                damage = 3,
                speed = 5,
                wepType = CombatEffect.None()
        ),
        maxHealth = 8,
        armor = 3,
        expGiven = 16
)

sealed class CombatEffect {
    class None:CombatEffect()
    class Stun(var chance:Int):CombatEffect()
}

class Weapon(var wepname:String,var damage:Int,var speed:Int,var wepType:CombatEffect)

open class Fightable(
        var name:String,
        var mainWeapon: Weapon,
        var maxHealth:Int,
        var armor:Int) {
        var currentHealth:Int = maxHealth
        var ailment:CombatEffect = CombatEffect.None()
        val healthStatus = {"${this.name} has ${this.currentHealth} currentHealth"}
}

open class Enemy(name: String,mainWeapon: Weapon,maxHealth: Int,armor: Int,var expGiven: Int):Fightable(name, mainWeapon, maxHealth, armor)

object UserInterface {
    var currentS :()->Scene = {Scene()}
    val gameForm = GameForm()
    val setupButtonListeners:()->Unit = {
        UserInterface.gameForm.button1.addActionListener { showScene(currentS().nextScene1) }
        UserInterface.gameForm.button2.addActionListener { showScene(currentS().nextScene2) }
        UserInterface.gameForm.button3.addActionListener { showScene(currentS().nextScene3) }
    }
    val showScene:(()->Scene)->Unit = { theScene->
        val ascene = theScene()
        ascene.runOnShow(ascene)
        currentS = {ascene}
        UserInterface.gameForm.button2.isVisible = ascene.numberOfButtons > 1
        UserInterface.gameForm.button3.isVisible = ascene.numberOfButtons > 2
        UserInterface.gameForm.textArea1.text = ascene.mainText
        UserInterface.gameForm.button1.text = ascene.button1Text
        UserInterface.gameForm.button2.text = ascene.button2Text
        UserInterface.gameForm.button3.text = ascene.button3Text
        UserInterface.gameForm.textArea2.text= "Leavel: ${Hero.heroLevel}\nExp: ${Hero.heroExp} \nMax Health: ${Hero.maxHealth}\nCurrent Health: ${Hero.currentHealth}\nArmour: ${Hero.armor}\n"
    }
}

object Hero:Fightable(
        name = "Our hero",
        mainWeapon =  Weapon("Starter knife",4,3,CombatEffect.None()),
        maxHealth = 10,
        armor = 5 ){
    var heroLevel = 1
    var heroExp = 0
    var lastCheckpointHero:Hero = this
    var lastCheckpointScene:Scene = WelcomeScene()
    var wand = false
    var leftHand:Weapon = Weapon("Starter buckler",1,9,CombatEffect.Stun(50))
    val expThreshes = listOf(4,15,30,60)
    val levelUp:(()->Unit,()->Unit)->Unit = { runOnLevel,runOnNoLevel->
        var didLevel = false
        expThreshes.forEachIndexed {index,thresh->
            if(thresh < heroExp && heroLevel<index+2){
                didLevel = true
                Hero.heroLevel = index+2
                Hero.maxHealth += index+2
            }
        }
        if(didLevel){
            Hero.currentHealth = Hero.maxHealth
            runOnLevel()
        } else runOnNoLevel()
    }
    val hitCheckpoint:(Scene)->Unit={scene->
        lastCheckpointScene = scene
        lastCheckpointHero = this
    }
    val loadCheckpointHero:()->Unit={
        heroExp = lastCheckpointHero.heroExp
        currentHealth = lastCheckpointHero.currentHealth
        armor = lastCheckpointHero.armor
        maxHealth = lastCheckpointHero.maxHealth
        heroLevel = lastCheckpointHero.heroLevel
    }
}

class WelcomeScene:Scene(
        mainText =  "You open your eyes and as the world comes into focus you become scared. You've never been to this place and have no memory of getting here. An alien plant bobbing nearby seems to contort and from it a voice emanates.\n" +
                "'Welcome, my child. May your stay here be less painful than it is for most.' The voice seems close, but the plant quickly softens and returns to its sunshine languishing.\nYou sit in shock for a moment before realising you have only two choices. In the near distance is an excavated bit of earth surrounded by crude palisade wall.\n" +
                "You can go check for any signs of civilised life, or you can sit here and waste away.",

        button1Text =  "Explore the pit",
        button2Text = "Sit in safety",
        nextScene1 = { CenterPitScene(mode = 1) },
        nextScene2 = { ScaredScene(WelcomeScene()) },
        runOnShow = {
            Hero.hitCheckpoint(WelcomeScene())
        }
)
class ScaredScene(backScene:Scene):Scene(
        mainText = "You sit frozen, unsure of where you are or what to do. After some time, the temporary peace becomes discomfort. Stand, ${Hero.name}, and by fate be borne on wings of fire!",
        nextScene1 = { backScene },
        button1Text = "Stand and explore",
        numberOfButtons = 1
)

class CenterPitScene(mode:Int):Scene(
        button1Text = "Fight the beast",
        button2Text = "Attempt to flee",
        nextScene1 = {
            FightScene(
                    enemy = Kobold(),
                    ongoing = false,
                    winScene = SidePitScene()
            )
        },
        nextScene2 = {FleePit()},
        runOnShow = {
            if(mode==1) it.mainText = "Barely more than a bit of cleared earth, the pit shows fresh signs of life. A dying fire. The Burnt bones of a recent meal.\n" +
                    "You smell the wretched danger before you see it. A panting Kobold springs into action, appearing in front of you with eyes flashing at the chance for a fight." +
                    "Its scarred, stinking body and jagged blade tell you there's no chance for diplomacy."
            if(mode==2) it.mainText = "Wand in hand, you return to the Pit where the Kobolds reside. Do you wish to try your luck with another beast or will you sit a moment to inspect the wand?"
            if(mode==3){
                it.mainText = "You return to the den of the Kobolds empty handed after exploring the ruins for a time. Do you wish to fight a wretched Kobold or return to the ruins?"
                it.button2Text = "Return to the altar"
                it.nextScene2 = {FindWandScene()}
            }
            if(Hero.wand){
                it.button2Text = "Draw your wand and examine the inscription"
                it.nextScene2 = {
                    MagicUpgradeScene(barracks(1))
                }
            }
        }
)

class FleePit:Scene(
        mainText = "You turn your back on the salivating wretch which seeks your life and move to escape. " +
                "Just as you turn to run, a greedy Kobold waiting for the outcome of the battle steps in your way holding a thin blade.\n" +
                "It slips in under your ribs, piercing your heart.",
        button1Text = "Next",
        nextScene1 = { DeathScene() },
        numberOfButtons = 1
)

class MagicUpgradeScene(backScene: Scene):Scene(
        mainText =  "You look closely upon the wand. The tiny etchings begin to glow with a thin silver light, moving across the surface of the wand. Coming from the centre of your belly, you feel an immense pull as if your entire being was drawn through the eye of needle." +
                "You find yourself in the realm of the Wand.\n" +
                "Without words, a humanoid form of pure light speaks to you through the mist. Towering above you, he lumbers slowly but you know, somehow, that he wishes you only kindness. You understand in an instant that he will heal your wounds and fortify your being.",
        button1Text = "Accept the Wandman's defensive enchantment",
        nextScene1 = {
            object:Scene(
                    nextScene1 =  { MagicUpgradeScene(backScene) },
                    button1Text = "Speak again to your magic friend",
                    numberOfButtons = 1,
                    runOnShow ={
                        if(Hero.armor < 6) {
                            Hero.armor++
                            it.mainText = "The entity encircles you with a whirling energy. Your skin and muscle harden in an instant. You feel as if your body has been tempered in a smith's fire abd your armour is now ${Hero.armor}."
                        }
                        else {
                            it.mainText = "You have already reaped the benefits of the fortifying enchantment."
                        }

                    }
            ){}
        },
        button2Text = "Seek healing magics",
        nextScene2 =  {
            object :Scene(
                nextScene1 =  { MagicUpgradeScene(backScene) },
                button1Text = "Communicate with the Entity",
                numberOfButtons = 1,
                runOnShow = {
                    if(Hero.currentHealth < Hero.maxHealth){
                        Hero.currentHealth = Hero.maxHealth
                        it.mainText =  "You are imbued with vitality. Your health is replenished." +
                                "Your health is now ${Hero.currentHealth}."
                    } else
                        it.mainText = "You are already completely healed. You return to the wand master."

                }
            ){}
        },
        nextScene3 =  { backScene },
        button3Text = "Leave the realm of the Wand",
        numberOfButtons = 3
)

class DeathScene:Scene(
        mainText =  "Your wounds are simply too great. You rest your head on the ground and your consciousness slips away.",
        nextScene1 =  {
            object :Scene(
                    mainText = "You awake to find your wounds healed, but your items are lost and your magical benefits removed.\n" +
                            "You will return to your last checkpoint",
                    button1Text = "Next",
                    nextScene1 = { Hero.lastCheckpointScene },
                    runOnShow = {
                        Hero.loadCheckpointHero()


                    },
                    numberOfButtons = 1
            ){}
        },
        button1Text = "Return to Checkpoint",
        numberOfButtons = 1
)

class FindWandScene:Scene(
        button1Text =  "Return to the Kobold encampment.",
        nextScene1 = {
            CenterPitScene(mode = 2)
        },
        numberOfButtons = 1,
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
        button1Text = "Search the barracks",
        button2Text = "Use the wand",
        nextScene2 = { MagicUpgradeScene(barracks(3)) },
        nextScene1 = {
            FightScene(
                    enemy = UndeadKnight(),
                    ongoing = false,
                    winScene = barracks(2)
            )
        },
        runOnShow = {
            Hero.hitCheckpoint(barracks(mode))
            if(mode==1) it.mainText ="You are thrown out of the Wandman's realm and find yourself standing in front of an ancient barracks. Do you wish to search the building or return to the safety of the Wandman's realm?"
            if(mode==2) {
                it.mainText ="You stand again before the barracks. Do you wish to challenge an Undead Knight or return to the safety of the Wandman's realm?"
                it.button1Text ="Fight an Undead Knight"
            }
            if(mode==3){
                it.mainText ="You are thrown out of the Wandman's realm and find yourself standing again before the barracks. Do you wish to challenge an Undead Knight or return back to the Wandman's realm?"
                it.button1Text ="Fight an Undead Knight"
            }
        }
)


class FightScene(enemy:Enemy,ongoing:Boolean,winScene:Scene): Scene(
        mainText = "A ${enemy.name} appears! ${enemy.healthStatus()}. ${Hero.healthStatus()}",
        button1Text = "use your ${Hero.leftHand.wepname}",
        button2Text = "use your ${Hero.mainWeapon.wepname}",
        nextScene1 = {
             if(Hero.leftHand.speed>enemy.mainWeapon.speed){
                 HitEnemyScene(enemy,GetHitScene(enemy,FightScene(enemy,true,winScene)),winScene,Hero.leftHand)
             }else{
                 GetHitScene(enemy,HitEnemyScene(enemy,FightScene(enemy,true,winScene),winScene,Hero.leftHand))
             }
        },
        nextScene2 = {
             if(Hero.mainWeapon.speed>enemy.mainWeapon.speed){
                 HitEnemyScene(enemy,GetHitScene(enemy,FightScene(enemy,true,winScene)),winScene,Hero.mainWeapon)
             }else{
                 GetHitScene(enemy, HitEnemyScene(enemy,  FightScene(enemy, true, winScene) , winScene, Hero.mainWeapon))
             }
         },
        runOnShow ={
            if(ongoing){
                it.mainText = "you continue ur battle with ${enemy.name}. ${enemy.healthStatus()}. ${Hero.healthStatus()}"
            }
        }
)

class HitEnemyScene(enemy: Enemy,responseScene:Scene,winScene: Scene,weapon: Weapon): Scene(
        numberOfButtons = 1,
        runOnShow = {
            val newEnemyHealth = enemy.currentHealth - weapon.damage
            val rand = Random().nextInt(100)
            val ail = Hero.ailment
            if(ail is CombatEffect.Stun && ail.chance>rand){
                it.mainText = "${Hero.name} is stunned and missed their turn!"
                it.button1Text = "damn"
                it.nextScene1 = {responseScene}
            }else if(newEnemyHealth<1){
                Hero.heroExp += enemy.expGiven
                Hero.levelUp(
                        {it.mainText = "You struck down the ${enemy.name} and level up! You now are level ${Hero.heroLevel}!."},
                        {it.mainText = "You struck down the ${enemy.name}! You now have ${Hero.heroExp} experience points!."}
                )
                it.nextScene1 = {winScene}
                it.button1Text = "awesome"

            }else{
                enemy.currentHealth = newEnemyHealth
                enemy.ailment = weapon.wepType
                it.mainText = "You hit the ${enemy.name} for ${weapon.damage} damage"
                it.nextScene1 = {responseScene}
                it.button1Text = "take that!"
            }
            if(Hero.ailment is CombatEffect.Stun) Hero.ailment = CombatEffect.None()
        }
)

class GetHitScene(enemy: Fightable,responseScene: Scene):Scene(
        numberOfButtons = 1,
        runOnShow ={
            val newhealth = Hero.currentHealth - enemy.mainWeapon.damage
            val rand = Random().nextInt(100)
            val ail = enemy.ailment
            if(ail is CombatEffect.Stun && ail.chance>rand){
                it.mainText = "the ${enemy.name} is stunned and missed their turn!"
                it.button1Text = "cool"
                it.nextScene1 = {responseScene}
            }else if(newhealth<1){
                it.mainText = "The ${enemy.name} hits you a fatal blow."
                it.nextScene1 = { DeathScene() }
                it.button1Text = "Next"
            }else{
                Hero.currentHealth = newhealth
                Hero.ailment = enemy.mainWeapon.wepType
                it.mainText = "The ${enemy.name} hits you for ${enemy.mainWeapon.damage} damage!"
                it.nextScene1 = {responseScene}
                it.button1Text = "I can handle it"
            }
            if(enemy.ailment is CombatEffect.Stun) enemy.ailment = CombatEffect.None()
        }
)

class SidePitScene:Scene(
        mainText = "Beyond the pit of Kobolds is an old burial mound, and upon it lies an altar.",
        button1Text = "Return to the Kobolds pit",
        button2Text = "Climb the mound and approach the altar.",
        nextScene1 = {CenterPitScene(mode = 3)},
        nextScene2 = {FindWandScene()}
)

class WinGameScene:Scene(
        mainText =  "You have conquered the mountain and reached the highest peak. You may now rest your head weary traveller. I am writing more to test out the line wrap functionality",
        nextScene1 =  { DeathScene() },
        button1Text = "I am the best!",
        numberOfButtons = 1
)