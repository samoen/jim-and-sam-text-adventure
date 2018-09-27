
import javax.swing.JFrame

class Launcher {
    companion object {
        @JvmStatic
        fun main(args: Array<String>){
            val frm = JFrame()
            frm.contentPane = UserInterface.gameForm.panel1
            frm.isVisible = true
            frm.setSize(500,500)
            WelcomeScene().ShowScene()
        }
    }
}

open class Item(var name:String)
enum class WepType{NORMAL,STUNNER}
class Weapon(var wepname: String,var damage: Int,var speed:Int,var wepType:WepType):Item(wepname){}

open class Fightable(
        var name:String="larry",
        var rightHand: Weapon = Weapon("starter knife",4,3,WepType.NORMAL),
        var health:Int=10
){
    fun HealthStatus() = "${this.name} has ${this.health} health"
}

object UserInterface{ val gameForm = GameForm() }

object Hero:Fightable(){
    var wand = false
    var leftHand:Weapon = Weapon("starter buckler",1,9,WepType.STUNNER)
}

class Enemy(name: String, weapon: Weapon, health: Int):Fightable(name, weapon, health)

class WelcomeScene():Scene(
    "Welcome son",
    "go into a dark pit",
    "refuse to endanger yourself",
    { CenterPitScene() },
    { ScaredScene() }
){
    init {
        Hero.health = 10
    }
}
class ScaredScene():Scene(
    mainText = "dont be scared, ${Hero.name}",
    nextScene1 = {
        UserInterface.gameForm.button2.isVisible = true
        WelcomeScene()
    },
    button1Text = "okay.."
){
    init {
        UserInterface.gameForm.button2.isVisible=false
    }
}

class CenterPitScene():Scene(
     "you in pit" ,
    "fight like man",
    "avoid danger",
    {
        FightScene(
            Enemy("kobold",Weapon("kobold dagger",3,5,WepType.NORMAL), 8),
            false,
            SidePitScene()
        )
    },
    {DeathScene()}
) {
    init {
        if(Hero.wand){
            button2Text = "use ur wand"
            nextScene2 = {
                WinGameScene()
            }
        }
    }
}

class DeathScene():Scene(
    mainText =  "you just straight up die, son",
    nextScene1 =  {
        UserInterface.gameForm.button2.isVisible = true
        WelcomeScene()
    },
    button1Text = "oh holy mama, restart me"
){
    init {
        UserInterface.gameForm.button2.isVisible = false
    }
}

class FindWandScene():Scene(
    button1Text =  "go back to the center of the pit",
    nextScene1 = {
        UserInterface.gameForm.button2.isVisible = true
        CenterPitScene()
    }
){
    init {
        UserInterface.gameForm.button2.isVisible = false
        if(Hero.wand){
            mainText = "you see the empty altar where the wand was"
        }else{
            mainText = "you sneak around and find a wand!"
            Hero.wand = true
        }
    }
}


class FightScene(enemy: Enemy,ongoing:Boolean,winScene:Scene): Scene(
    "A ${enemy.name} appears! ${enemy.HealthStatus()}. ${Hero.HealthStatus()}",
    "use your ${Hero.leftHand.name}",
    "use your ${Hero.rightHand.name}",
     {
         if(Hero.leftHand.speed>enemy.rightHand.speed){
             HitEnemyScene(enemy,{GetHitScene(enemy,FightScene(enemy,true,winScene))},winScene,Hero.leftHand)
         }else{
             GetHitScene(enemy,HitEnemyScene(enemy,{FightScene(enemy,true,winScene)},winScene,Hero.leftHand) )
         }
     },
     {
         if(Hero.rightHand.speed>enemy.rightHand.speed){
             HitEnemyScene(enemy,{GetHitScene(enemy,FightScene(enemy,true,winScene))},winScene,Hero.rightHand)
         }else{
             GetHitScene(enemy,HitEnemyScene(enemy,{FightScene(enemy,true,winScene)},winScene,Hero.rightHand) )
         }
     }
){
    init {
        if(ongoing){
            mainText = "you continue ur battle with ${enemy.name}. ${enemy.HealthStatus()}. ${Hero.HealthStatus()}"
        }

    }

}

class HitEnemyScene(enemy: Enemy,responseScene:()->Scene,winScene: Scene,weapon: Weapon): Scene(){
    init {
        val newEnemyHealth = enemy.health - weapon.damage
        if(newEnemyHealth<1){
            mainText = "You struck down the ${enemy.name}!"
            nextScene1 = {winScene}
            button1Text = "awesome"
        }else{
            enemy.health = newEnemyHealth
            mainText = "You hit the ${enemy.name} for ${weapon.damage} damage"
            nextScene1 = responseScene
            button1Text = "take that!"
        }
    }
}

class GetHitScene(enemy: Enemy,responseScene: Scene):Scene(){
    init {
        UserInterface.gameForm.button2.isVisible = false
        val newhealth = Hero.health - enemy.rightHand.damage
        if(newhealth<1){
            mainText = "you succumb to your wounds and die son"
            nextScene1 = {
                UserInterface.gameForm.button2.isVisible = true
                WelcomeScene()
            }
            button1Text = "awww :("
        }else{
            Hero.health = newhealth
            mainText = "The ${enemy.name} hits you, your health is now ${Hero.health}"
            nextScene1 = {
                UserInterface.gameForm.button2.isVisible = true
                responseScene
            }
            button1Text = "I can handle it"
        }
    }
}

class SidePitScene():Scene(
        "You look around and see many small passages. behind you is the pit center",
        "go back center pit",
        "sneak around",
        {CenterPitScene()},
        {FindWandScene()}
)

class WinGameScene():Scene(
        mainText =  "You have conquered the mountain and\nreached the highest peak. You may now rest\nyour head weary traveeller",
        nextScene1 =  {
            UserInterface.gameForm.button2.isVisible = true
            WelcomeScene()
        },
        button1Text = "I am the best!"
){
    init {
        UserInterface.gameForm.button2.isVisible = false
    }
}