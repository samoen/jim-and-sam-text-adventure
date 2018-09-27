import java.awt.event.ActionEvent

open class Scene(
        var mainText:String="",
        var button1Text:String="",
        var button2Text:String="",
        var nextScene1:()->Scene={Scene()},
        var nextScene2:()->Scene={Scene()},
        var numberOfButtons:Int = 2
)
{
    fun ShowScene(){
        if(numberOfButtons == 2){
            UserInterface.gameForm.button2.isVisible = true
        }else{
            if(UserInterface.gameForm.button2.isVisible)
                UserInterface.gameForm.button2.isVisible = false
        }

        UserInterface.gameForm.textArea1.text = mainText
        UserInterface.gameForm.button1.text = button1Text
        UserInterface.gameForm.button2.text = button2Text
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
    }
}