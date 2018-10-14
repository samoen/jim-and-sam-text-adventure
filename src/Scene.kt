import java.awt.event.ActionEvent

open class Scene(
        var mainText:String="",
        var button1Text:String="",
        var button2Text:String="",
        var button3Text:String="",
        var nextScene1:()->Scene={Scene()},
        var nextScene2:()->Scene={Scene()},
        var nextScene3:()->Scene={Scene()},
        var numberOfButtons:Int = 2,
        var runOnShow:(Scene)->Unit = {}
){
    fun ShowScene(){
        runOnShow.invoke(this)
        UserInterface.gameForm.button2.isVisible = true
        UserInterface.gameForm.button3.isVisible = true
        UserInterface.gameForm.button3.isVisible = true

        if(numberOfButtons == 2){
            UserInterface.gameForm.button3.isVisible = false
        }else if(numberOfButtons == 1){
            UserInterface.gameForm.button2.isVisible = false
            UserInterface.gameForm.button3.isVisible = false
        }else if (numberOfButtons == 3){

        }
        UserInterface.gameForm.textArea1.text = mainText
        UserInterface.gameForm.button1.text = button1Text
        UserInterface.gameForm.button2.text = button2Text
        UserInterface.gameForm.button3.text = button3Text
        for (actionListener in UserInterface.gameForm.button2.actionListeners) {
            UserInterface.gameForm.button2.removeActionListener(actionListener)
        }
        UserInterface.gameForm.button2.addActionListener { _: ActionEvent ->
            nextScene2().ShowScene()
        }
        for (actionListener in UserInterface.gameForm.button1.actionListeners) {
            UserInterface.gameForm.button1.removeActionListener(actionListener)
        }
        UserInterface.gameForm.button1.addActionListener { _: ActionEvent ->
            nextScene1().ShowScene()
        }
        for (actionListener in UserInterface.gameForm.button3.actionListeners) {
            UserInterface.gameForm.button3.removeActionListener(actionListener)
        }
        UserInterface.gameForm.button3.addActionListener { _: ActionEvent ->
            nextScene3().ShowScene()
        }
        UserInterface.gameForm.textArea2.text= "Leavel: ${Hero.heroLevel}\nExp: ${Hero.heroExp} \nMax Health: ${Hero.maxHealth}\nCurrent Health: ${Hero.currentHealth}\nArmour: ${Hero.armor}\n"
    }
}