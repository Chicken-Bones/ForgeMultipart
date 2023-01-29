package codechicken

import codechicken.microblock.handler.MicroblockProxy

package object microblock {
  def logger = MicroblockProxy.logger
}
