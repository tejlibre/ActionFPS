package com.actionfps.ladder.parser

/**
  * Created by me on 02/05/2016.
  */
case class UserStatistics(frags: Int, gibs: Int, flags: Int) {
  def kill = copy(frags = frags + 1)

  def gib = copy(gibs = gibs + 1)

  def flag = copy(flags = flags + 1)

  def points = (2 * frags) + (3 * gibs) + (15 * flags)
}

object UserStatistics {
  def empty = UserStatistics(frags = 0, gibs = 0, flags = 0)
}