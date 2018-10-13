
import java.util.*
import javax.swing.JFrame

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>){
            val frm = JFrame()
            frm.contentPane = UserInterface.gameForm.panel1
            frm.isVisible = true
            frm.setSize(1000,1000)
            WelcomeScene().ShowScene()
        }
    }
}

open class Item(var name:String)

sealed class CombatEffect(){
    class None:CombatEffect()
    class Stun(var chance:Int):CombatEffect()
}

class Weapon(var wepname: String,var damage: Int,var speed:Int,var wepType:CombatEffect):Item(wepname){}

open class Fightable(
        var name:String,
        var mainWeapon: Weapon,
        var health:Int
){
    var ailment:CombatEffect = CombatEffect.None()
    fun HealthStatus() = "${this.name} has ${this.health} health"
}

object UserInterface{ val gameForm = GameForm() }

object Hero:Fightable(
        name = "Our hero",
        mainWeapon =  Weapon("Starter knife",4,3,CombatEffect.None()),
        health = 10
){
    var wand = false
    var leftHand:Weapon = Weapon("Starter buckler",1,9,CombatEffect.Stun(50))
}

class WelcomeScene():Scene(
    mainText =  "Welcome, my child. May your stay here be less painful than it is for most.",
    button1Text =  "go into a dark pit",
    button2Text = "refuse to endanger yourself",
    nextScene1 = { CenterPitScene() },
    nextScene2 = { ScaredScene() },
    runOnShow ={
        Hero.health = 10
    }
)
class ScaredScene():Scene(
    mainText = "dont be scared, ${Hero.name}",
    nextScene1 = { WelcomeScene() },
    button1Text = "okay..",
    numberOfButtons = 1
)

class CenterPitScene():Scene(
    mainText = "you in pit" ,
    button1Text = "fight like man",
    button2Text = "avoid danger",
    nextScene1 = {
        FightScene(
                Fightable("kobold",Weapon("kobold dagger",3,5,CombatEffect.None()), 8),
                false,
                SidePitScene()
        )
    },
    nextScene2 = {DeathScene()},
    runOnShow = {
        if(Hero.wand){
            it.button2Text = "use ur wand"
            it.nextScene2 = {
                MagicUpgradeScene()
            }
        }
    }
)

class MagicUpgradeScene():Scene(
        mainText =  "The wand presents you with an epic choice, that will determine it's effect..... If you awoke on a deserted island would you:",
        nextScene1 =  { MagicDefenderScene() },
        nextScene2 =  { MagicAttackerScene() },
        button1Text = "Find water and build shelter",
        button2Text = "Start a fire and cook some meat",
        numberOfButtons = 2,
        runOnShow ={
            Hero.health = 10
        }


)

class MagicDefenderScene():Scene(
        mainText =  "You are embued with defensive power, you deal extra damage with your off hand weapon and are fully healed",
        nextScene1 =  { CenterPitScene() },
        button1Text = "Go back to the pit",
        numberOfButtons = 1,
        runOnShow ={
            Hero.leftHand.damage ++
        }

)

class MagicAttackerScene():Scene(
        mainText =  "You are embued with offensive power, you deal extra damage with your off hand weapon and are fully healed",
        nextScene1 =  { CenterPitScene() },
        button1Text = "Go back to the pit",
        numberOfButtons = 1,
        runOnShow = {
            Hero.mainWeapon.damage++
        }
)


class DeathScene():Scene(
    mainText =  "you just straight up die, son",
    nextScene1 =  { WelcomeScene() },
    button1Text = "oh holy mama, restart me",
    numberOfButtons = 1
)

class FindWandScene():Scene(
    button1Text =  "go back to the center of the pit",
    nextScene1 = {
        CenterPitScene()
    },
    numberOfButtons = 1,
    runOnShow = {
        if(Hero.wand){
            it.mainText = "you see the empty altar where the wand was"
        }else{
            it.mainText = "you sneak around and find a wand!"
            Hero.wand = true
        }
    }
)

class FightScene(enemy: Fightable,ongoing:Boolean,winScene:Scene): Scene(
    mainText = "A ${enemy.name} appears! ${enemy.HealthStatus()}. ${Hero.HealthStatus()}",
    button1Text = "use your ${Hero.leftHand.name}",
    button2Text = "use your ${Hero.mainWeapon.name}",
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
            it.mainText = "you continue ur battle with ${enemy.name}. ${enemy.HealthStatus()}. ${Hero.HealthStatus()}"
        }
    }
)

class HitEnemyScene(enemy: Fightable,responseScene:Scene,winScene: Scene,weapon: Weapon): Scene(
        numberOfButtons = 1,
        runOnShow = {
            val newEnemyHealth = enemy.health - weapon.damage
            val rand = Random().nextInt(100)
            val ail = Hero.ailment
            if(ail is CombatEffect.Stun && ail.chance>rand){
                it.mainText = "${Hero.name} is stunned and missed their turn!"
                it.button1Text = "damn"
                it.nextScene1 = {responseScene}
            }else if(newEnemyHealth<1){
                it.mainText = "You struck down the ${enemy.name}!"
                it.nextScene1 = {winScene}
                it.button1Text = "awesome"
            }else{
                enemy.health = newEnemyHealth
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
            val newhealth = Hero.health - enemy.mainWeapon.damage
            val rand = Random().nextInt(100)
            val ail = enemy.ailment
            if(ail is CombatEffect.Stun && ail.chance>rand){
                it.mainText = "the ${enemy.name} is stunned and missed their turn!"
                it.button1Text = "cool"
                it.nextScene1 = {responseScene}
            }else if(newhealth<1){
                it.mainText = "you succumb to your wounds and die son"
                it.nextScene1 = { WelcomeScene() }
                it.button1Text = "awww :("
            }else{
                Hero.health = newhealth
                Hero.ailment = enemy.mainWeapon.wepType
                it.mainText = "The ${enemy.name} hits you, your health is now ${Hero.health}"
                it.nextScene1 = {responseScene}
                it.button1Text = "I can handle it"
            }
            if(enemy.ailment is CombatEffect.Stun) enemy.ailment = CombatEffect.None()
        }
)

class SidePitScene():Scene(
        mainText = "You look around and see many small passages. behind you is the pit center",
        button1Text = "go back center pit",
        button2Text = "sneak around",
        nextScene1 = {CenterPitScene()},
        nextScene2 = {FindWandScene()}
)

class WinGameScene():Scene(
        mainText =  "You have conquered the mountain and reached the highest peak. You may now rest your head weary traveller. I am writing more to test out the line wrap functionality",
        nextScene1 =  { WelcomeScene() },
        button1Text = "I am the best!",
        numberOfButtons = 1
)