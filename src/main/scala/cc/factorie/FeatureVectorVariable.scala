/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie
import cc.factorie.la._
import scala.util.MurmurHash
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.collection.mutable.SynchronizedMap

trait FeatureVectorVar[-C] extends DiscreteDimensionTensorVar {
  def +=(elt:C, incr:Double): Unit
  def +=(elt:C): Unit
  def ++=(elts:Iterable[C]): Unit
}

/** The standard variable for holding binary feature vectors.
    It is a CategoricalDimensionTensorVariable initialized with a GrowableSparseBinaryTensor1 value.
    @author Andrew McCallum */
abstract class BinaryFeatureVectorVariable[C] extends CategoricalDimensionTensorVariable[C] with FeatureVectorVar[C] {
  def this(initVals:Iterable[C]) = { this(); this.++=(initVals) }
  set(new GrowableSparseBinaryTensor1(domain.dimensionDomain))(null)
  override def toString: String = activeCategories.mkString(printName+"(", ",", ")")
}

/** The standard variable for holding feature vectors with non-binary values.
    It is a CategoricalDimensionTensorVariable initialized with a GrowableSparseBinaryTensor1 value.
    @author Andrew McCallum */
abstract class FeatureVectorVariable[C] extends CategoricalDimensionTensorVariable[C] with FeatureVectorVar[C] {
  def this(initVals:Iterable[C]) = { this(); this.++=(initVals) }
  set(new GrowableSparseTensor1(domain.dimensionDomain))(null)
  override def toString: String = {
    val b = new StringBuilder; b append printName; b append "("
    tensor.foreachActiveElement((i,v) => {
      b append domain.dimensionDomain.category(i)
      b append "="; b append v; b append ","
    })
    b.dropRight(1); b.append(")"); b.toString
  } 
}


/** Functions used inside HashFeatureVectorVariable,
    also available here for outside use. */
object HashFeatureVectorVariable {
  def hash(c: Any): Int = c match {
    case s:String => MurmurHash.stringHash(s)
    case _ => c.hashCode()
  }
  def index(c: Any, size: Int): Int = hash(c) % size
  def sign(c: Any): Int = hash(c) >> 31
}

/** A variable whose value is a SparseTensor1 whose length matches the size of a DiscreteDomain,
    and whose dimensions each correspond to the result of running a hash function on elements
    added to the vector using +=.
    These can be used as feature vectors where one wants to avoid a large or growing CategoricalDomain.
    The 'dimensionDomain' is abstract.
    @author Andrew McCallum */
abstract class HashFeatureVectorVariable extends DiscreteDimensionTensorVariable with FeatureVectorVar[Any] {
  override def domain: DiscreteDomain
  def this(initVals:Iterable[Any]) = { this(); initVals.map(this.+=_) }
  def ++=(cs: Iterable[Any]): Unit = cs.foreach(this.+= _)
  def +=(c: Any): Unit = {
    val hash = HashFeatureVectorVariable.hash(c)
    val sign = math.signum(hash >> 31)
    tensor.update(math.abs(hash % domain.size), sign)
  }
  set(new SparseIndexedTensor1(domain.size))(null)
}
