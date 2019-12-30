package org.ro.layout

import kotlinx.serialization.Serializable
import org.ro.to.Link
import org.ro.to.Member
import org.ro.to.bs3.Col
import pl.treksoft.kvision.core.Component
import pl.treksoft.kvision.panel.HPanel

@Serializable
data class ColLayout(val domainObject: DomainObjectLayout? = null,
                     val action: MutableList<ActionLayout> = mutableListOf<ActionLayout>(),  // org.ro.authors
                     val named: String? = "",
                     val describedAs: String? = "",
                     val plural: String? = "",
                     val link: Link? = null,
                     val bookmarking: String? = "",
                     val metadataError: String? = "",
                     val cssClass: String? = "",
                     val cssClassFa: String? = "",
                     val cssClassFaPosition: String? = "",
                     val namedEscaped: Boolean? = false,
                     val size: String? = "",
                     val id: String? = "",
                     val span: Int? = 0,
                     val unreferencedActions: Boolean? = false,
                     val unreferencedCollections: Boolean? = false,
                     val tabGroup: MutableList<TabGroupLayout> = mutableListOf<TabGroupLayout>(),
                     val fieldSet: MutableList<FieldSetLayout> = mutableListOf<FieldSetLayout>()
) {
    constructor(col: Col) : this() {
        col.tabGroups.forEach {
            tabGroup.add(TabGroupLayout(it))
        }
        val fs = col.fieldSet!!
        fieldSet.add(FieldSetLayout(fs))
    }

    fun build(members : Map<String, Member>): HPanel {
        val result = HPanel()
        var cpt: Component?
        for (tgl in tabGroup) {
            cpt = tgl.build(members)
            result.add(cpt)
        }
        for (fsl in fieldSet) {
            cpt = fsl.build(members)
            result.add(cpt!!)
        }
        return result
    }

}
