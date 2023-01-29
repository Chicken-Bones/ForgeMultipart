package codechicken.multipart

/** Interface for parts with random update ticks. Used in conjuction with
  * TickScheduler
  */
trait TRandomUpdateTick extends TMultiPart {

  /** Called on random update. Random ticks are between 800 and 1600 ticks from
    * their last scheduled/random tick
    */
  def randomUpdate()

  /** If implementing interface in java, be sure to implement this method
    * yourself
    */
  override def onWorldJoin() {
    TickScheduler.loadRandomTick(this)
  }
}
