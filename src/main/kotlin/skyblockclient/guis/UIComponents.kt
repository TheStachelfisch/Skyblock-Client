package skyblockclient.guis

import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color

class UIButton(
    text: String,
    round: Float = 0f,
    color: Color = Color(0, 0, 0, 64),
    hoverColor: Color = Color(128, 128, 128, 128)
) : UIRoundedRectangle(round) {
    private val uiText = UIText(text).constrain {
        x = CenterConstraint()
        y = CenterConstraint()
    } childOf this

    init {
        this.constrain {
            width = (uiText.getWidth() + 40).pixels()
            height = (uiText.getHeight() + 10).pixels()
        }.onMouseEnter {
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    0.5f,
                    hoverColor.toConstraint()
                )
            }
        }.onMouseLeave {
            animate {
                setColorAnimation(
                    Animations.OUT_EXP,
                    0.5f,
                    color.toConstraint()
                )
            }
        }.setColor(color)
    }
}