package codechicken.microblock

import codechicken.multipart.MultiPartRegistry
import codechicken.multipart.MultiPartRegistry.IPartFactory2
import net.minecraft.nbt.NBTTagCompound
import codechicken.lib.data.MCDataInput
import cpw.mods.fml.relauncher.{Side, SideOnly}

abstract class MicroblockClass extends IPartFactory2 {
  def getName: String
  def baseTrait: Class[_ <: Microblock]
  @SideOnly(Side.CLIENT)
  def clientTrait: Class[_ <: MicroblockClient]
  def getResistanceFactor: Float

  val baseTraitId = MicroblockGenerator.registerTrait(baseTrait)
  @SideOnly(Side.CLIENT)
  lazy val clientTraitId = MicroblockGenerator.registerTrait(clientTrait)

  def register() {
    MultiPartRegistry.registerParts(this, getName)
  }

  def create(client: Boolean, material: Int) =
    MicroblockGenerator.create(this, material, client)
  override def createPart(name: String, packet: MCDataInput) =
    create(true, MicroMaterialRegistry.readMaterialID(packet))
  override def createPart(name: String, nbt: NBTTagCompound) =
    create(false, MicroMaterialRegistry.materialID(nbt.getString("material")))
}

/** Microblocks with corresponding items
  */
abstract class CommonMicroClass extends MicroblockClass {
  private var classId: Int = _

  def getClassId = classId
  def itemSlot: Int
  def placementProperties: PlacementProperties

  def register(id: Int) {
    register()
    classId = id
    CommonMicroClass.registerMicroClass(this, id)
  }
}

object CommonMicroClass {
  val classes = new Array[CommonMicroClass](256)

  def getMicroClass(modelId: Int) = classes(modelId >> 8)

  def registerMicroClass(mcrClass: CommonMicroClass, id: Int) {
    if (classes(id) != null)
      throw new IllegalArgumentException(
        "Microblock class id " + id + " is already taken by " + classes(
          id
        ).getName + " when adding " + mcrClass.getName
      )

    classes(id) = mcrClass
  }
}
