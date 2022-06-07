package com.github.mnemotechnician.kmmt

import arc.*
import arc.scene.ui.*
import mindustry.mod.*
import mindustry.game.*
import mindustry.ui.*
import mindustry.ui.dialogs.*
import com.github.mnemotechnician.mkui.*

class ExampleMod : Mod() {

	val info = """
		This is a mod template.
		
		If you've installed this mod by accident, just uninstall it.
		This mod doesn't add anything useful
		It's just an template for mod developers.
	""".trimIndent()

	init {
		//when client load event is fired (that happens only once),
		Events.on(EventType.ClientLoadEvent::class.java) {
			//create a dialog,
			val dialog = BaseDialog("This is an example mod")
			//make the dialog close when the player presses escape / back key
			dialog.closeOnBack()
			 
			dialog.cont.apply {
				//add an info label
				addLabel(info).marginBottom(20f).row()
				
				//and a button group...
				buttonGroup {
					defaults().width(120f)
					
					//containing two buttons that put different messages on the label
					textButton("info", Styles.togglet) {
						dialog.cont.childAs<Label>(0).setText(info)
					}
					
					textButton("about us", Styles.togglet) {
						dialog.cont.childAs<Label>(0).setText("""
							[Kotlin] Mindustry Mod Template by Mnemotechnician
							
							Discord: @Mnemotechnician#9967
							Github: https://github.com/Mnemotechnician
						""".trimIndent())
					}
				}.marginBottom(60f).row()
				
				//and don't forget to add a close button to the dialog
				textButton("close", Styles.nodet) { dialog.hide() }.width(240f)
			}
			
			dialog.show()
		}
	}
	
}
