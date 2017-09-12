package knapsack.solver

import java.util
import java.util.Collections

import knapsack.domain.{Item, KnapsackResult}
import knapsack.solver.LinearRelaxationEstimator.Estimate

import scala.annotation.tailrec
import scala.collection.JavaConversions._
import scala.collection.mutable

object Domain {
  type ItemIndex = Int
  type IndexedItem = (Item, ItemIndex)
  type ItemStatus = Option[Boolean]
  type Value = Int
  type Solution = List[Boolean]
  type PartialSolution = Map[ItemIndex, Boolean]

  case class Node(value: Int, capacity: Int, estimate: Estimate, currentSolution: PartialSolution) {
    val isValid: Boolean = capacity >= 0
    val height: Int = currentSolution.size + 1
  }
}

object LinearRelaxationEstimator {
  import Domain._

  sealed trait Estimate {
    def value: Double
  }
  case class CriticalItem(value: Double, criticalItem: (Item, ItemIndex)) extends Estimate
  case class NonCritical(value: Double, solution: PartialSolution) extends Estimate

  object Estimate {
    def empty: NonCritical = NonCritical(0, Map.empty)
  }

  def calculateEstimate(currentValue: Double, capacity: Int, items: List[(Item, ItemIndex)]): Estimate = {
    calculateEstimateHelper(currentValue, capacity, items, Map.empty)
  }

  @tailrec
  private def calculateEstimateHelper(currentValue: Double, capacity: Int, itemsLeft: List[(Item, ItemIndex)], solution: PartialSolution): Estimate =
    itemsLeft match {
      case Nil => NonCritical(currentValue, solution)
      case (item, index) :: restOfItems =>
        if (item.getWeight >= capacity) CriticalItem(currentValue + item.getValue * (capacity.toDouble / item.getWeight), (item, index))
        else calculateEstimateHelper(currentValue + item.getValue, capacity - item.getWeight, restOfItems, solution + (index -> true))
    }
}

sealed trait BranchAndBoundSolver extends KnapsackSolver {
  import LinearRelaxationEstimator._
  import Domain._

  def nodeOrdering: Ordering[Node]

  private def valueToWeightRatio(item: Item) = item.getValue.toDouble / item.getWeight

  private val itemOrderingByValueToWeightRatio = new Ordering[Item] {
    override def compare(x: Item, y: Item): Int = {
      val valueToWeightDifference = valueToWeightRatio(x) - valueToWeightRatio(y)
      if (valueToWeightDifference == 0) x.getValue - y.getValue
      else valueToWeightDifference.signum
    }
  }

  def solve(maxWeight: Int, items: util.List[knapsack.domain.Item]): KnapsackResult = {
    val sortedItems = items.sorted(itemOrderingByValueToWeightRatio.reverse)
    val estimate = calculateEstimate(0, maxWeight, sortedItems.zipWithIndex.toList)
    val rootNode = Node(0, maxWeight, estimate, Map.empty)
    val nodeQueue = mutable.PriorityQueue(rootNode)(nodeOrdering)
    val (lowerBoundValue, lowerBoundSolution) = pickInitialGuess(maxWeight, sortedItems.toList)
    if (lowerBoundSolution.forAll(!_))
      
    val solution = lowerBoundGuess.map(solve(sortedItems, nodeQueue, _))
      .getOrElse(Node(0, maxWeight, Estimate.empty, Array.fill(sortedItems.length)(Some(false))))
    val itemsInSolution = sortedItems.zip(solution.currentSolutionBitmap)
      .filter { case(_, status) => status.exists(identity) }
      .map(_._1)
    new KnapsackResult (solution.value, itemsInSolution.toList, true)
  }

  private def pickInitialGuess(maxWeight: Int, sortedItems: List[Item]): (Value, Solution) = {
    val (finalValue, _, reversedSolution) = sortedItems.foldLeft((0, maxWeight, List[Boolean]())) {
      case ((value, capacity, solution), item) =>
        if (item.getWeight > capacity) (value, capacity, false :: solution)
        else (value + item.getValue, capacity - item.getWeight, true :: solution)
    }
    (finalValue, reversedSolution.reverse)
  }

  @tailrec
  private def solve(sortedItems: Array[Item], queue: mutable.PriorityQueue[Node], bestValue: Value, bestSolution: Solution): (Value, Solution) = {
    if (queue.isEmpty) (bestValue, bestSolution)
    else {
      val currentNode = queue.dequeue
      if (bestValue >= currentNode.estimate.value) {
        solve(sortedItems, queue, bestValue, bestSolution)
      } else {
        currentNode.estimate match {
          case NonCritical(value, partialSolution) => solve(sortedItems, queue, value.toInt, toSolution(mergeMaps(partialSolution, currentNode.currentSolution)))
          case CriticalItem(_, (_, index)) =>
            val filteredChildren = calculateChildren(currentNode, sortedItems, index).filter(_.isValid)
            if (childrenAreLeafNodes(currentNode, sortedItems.length)) {
              val nodesToCheck = (bestValue, bestSolution) :: filteredChildren.map(child => (child.value, child.currentSolution.toList.sortBy(_._1).map(_._2)))
              val (newBestValue, newBestSolution) = nodesToCheck.maxBy(_._1)
              solve(sortedItems, queue, newBestValue, newBestSolution)
            } else {
              filteredChildren.foreach(queue enqueue _)
              solve(sortedItems, queue, bestValue, bestSolution)
            }
          }
        }
      }
    }

  private def mergeMaps[K, V](map1: Map[K, V], map2: Map[K, V]): Map[K, V] = {
    map1.foldRight(map2) { case ((key, value), mergedMap) => mergedMap + (key -> value)}
  }

  private def toSolution(partialSolution: PartialSolution): Solution = {
    partialSolution.toList.sortBy(_._1).map(_._2)
  }

  private def childrenAreLeafNodes(parent: Node, numberOfItems: Int): Boolean =  parent.height == numberOfItems

  private def calculateChildren(node: Node, items: Array[Item], criticalItemIndex: Int): List[Node] = {
    val currentSolutionForLeftChild = node.currentSolution + (criticalItemIndex -> true)
    val criticalItem = items(criticalItemIndex)
    val leftNodeUpdatedValue = node.value + criticalItem.getValue
    val leftNodeUpdatedCapacity = node.capacity - criticalItem.getWeight
    val leftNodeItemsLeft = items.zipWithIndex.filter { case (_, index) => currentSolutionForLeftChild.get(index).forall(!_) }
    val leftNode = node.copy(
      value = leftNodeUpdatedValue,
      capacity = leftNodeUpdatedCapacity,
      estimate = calculateEstimate(leftNodeUpdatedValue, leftNodeUpdatedCapacity, leftNodeItemsLeft.toList),
      currentSolution = currentSolutionForLeftChild)
    val (beforeCriticalItem, afterCriticalItem) = leftNodeItemsLeft.partition(_._2 == criticalItemIndex)
    val rightNode = node.copy(
      estimate = calculateEstimate(node.value, node.capacity, (beforeCriticalItem ++ afterCriticalItem).toList),
      currentSolution = node.currentSolution + (criticalItemIndex -> false))
    List(leftNode, rightNode)
  }
}

object DfsBranchAndBoundSolver extends BranchAndBoundSolver {
  import Domain.Node

  override val nodeOrdering: Ordering[Node] = new Ordering[Node] {
    override def compare(x: Node, y: Node): Int = x.height - y.height
  }
}

object BfsBranchAndBoundSolver extends BranchAndBoundSolver {
  import Domain.Node

  override val nodeOrdering = new Ordering[Node] {
    override def compare(x: Node, y: Node): Int = (x.estimate.value - y.estimate.value).signum
  }
}

class LeastDiscrepancySolver extends KnapsackSolver {

  case class Result(value: Int, capacity: Int, solution: List[Boolean])

  override def solve(maxWeight: Int, itemList: util.List[Item]): KnapsackResult = {
    val items = itemList.toList.sortBy(item => item.getValue.toDouble / item.getWeight)(Ordering[Double].reverse)
    val bestResult = (0 to items.size).foldRight(Option.empty[Result])(solve(maxWeight, items, _, _))
    bestResult match {
      case None => new KnapsackResult(0, Collections.emptyList(), true)
      case Some(result) =>
        val itemsInSolution = items.zip(result.solution)
          .filter(_._2)
          .map(_._1)
        new KnapsackResult(result.value, itemsInSolution, true)
    }
  }

  private def solve(maxWeight: Int, items: List[Item], numItemsToTake: Int, bestResult: Option[Result]): Option[Result] = {
    if (numItemsToTake < 0) bestResult
    else {
      val initialSolutionBitmap: List[Boolean] = List.fill[Boolean](numItemsToTake)(true) ::: List.fill[Boolean](items.size - numItemsToTake)(false)
      initialSolutionBitmap.permutations.foldRight(bestResult) {
        case(solution, currentBestResult) =>
          val possibleCurrentResult = calculateResult(maxWeight, solution, items)
          pickBestPotentialResult(currentBestResult, possibleCurrentResult)
      }
    }
  }

  private def pickBestPotentialResult(firstResultOption: Option[Result], secondResultOption: Option[Result]) = {
    firstResultOption.filter(firstResult => secondResultOption.forall(_.value <= firstResult.value)).orElse(secondResultOption)
  }

  private def calculateResult(maxWeight: Int, proposedSolution: List[Boolean], items: List[Item]): Option[Result] = {
    val solutionItems = proposedSolution.toStream.zip(items).filter(_._1).map(_._2)
    solutionItems.foldLeft(Option(Result(0, maxWeight, proposedSolution))) {
      case (resultOpt, item) => resultOpt.flatMap(result => {
        val newCapacity = result.capacity - item.getWeight
        if (newCapacity < 0) None
        else {
          val newValue = result.value + item.getValue
          Some(Result(newValue, newCapacity, proposedSolution))
        }
      })
    }
  }
}

