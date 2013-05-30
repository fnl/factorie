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

package cc.factorie.app.nlp
import cc.factorie._
import scala.collection.mutable.ArrayBuffer

class Sentence(sec:Section, initialStart:Int, initialLength:Int)(implicit d:DiffList = null) extends TokenSpan(sec, initialStart, initialLength)(d) {
  def this(sec:Section)(implicit d:DiffList = null) = this(sec, sec.length, 0)
  def this(doc:Document)(implicit d:DiffList = null) = this(doc.wholeDocumentSection)

  if (!sec.document.annotators.contains(classOf[Sentence])) sec.document.annotators(classOf[Sentence]) = null

  //def tokens: IndexedSeq[Token] = links
  def tokenAtCharIndex(charOffset:Int): Token = {
    require(charOffset >= tokens.head.stringStart && charOffset <= tokens.last.stringEnd)
    var i = 0 // TODO Implement as faster binary search
    while (i < this.length && tokens(i).stringStart <= charOffset) {
      val token = tokens(i)
      //if (token.stringStart <= charOffset && token.stringEnd <= charOffset) return token
      if (token.stringStart <= charOffset && token.stringEnd >= charOffset) return token
      i += 1
    }
    return null
  }

  def contains(elem: Token) = tokens.contains(elem)

  // Parse attributes
  def parse = attr[cc.factorie.app.nlp.parse.ParseTree]
  def parseRootChild: Token = attr[cc.factorie.app.nlp.parse.ParseTree].rootChild

  // common labels
  def posLabels = tokens.map(_.posLabel)
  def nerLabels = tokens.map(_.nerLabel)
}


// Cubbie storage

class SentenceCubbie extends TokenSpanCubbie {
  def finishStoreSentence(s:Sentence): Unit = {}
  def storeSentence(s:Sentence): this.type = {
	storeTokenSpan(s) // also calls finishStoreTokenSpan(s)
    finishStoreSentence(s)
    this
  }
  def finishFetchSentence(s:Sentence): Unit = finishFetchTokenSpan(s)
  def fetchSentence(section:Section): Sentence = {
    val s = new Sentence(section, start.value, length.value)
    finishFetchSentence(s)
    s
  }
}

// To save the sentence with its parse tree use "new SentenceCubbie with SentenceParseTreeCubbie"
trait SentenceParseCubbie extends SentenceCubbie {
  val parse = CubbieSlot("parse", () => new cc.factorie.app.nlp.parse.ParseTreeCubbie)
  override def finishStoreSentence(s:Sentence): Unit = {
    super.finishStoreSentence(s)
    parse := parse.constructor().storeParseTree(s.parse)
  }
  override def finishFetchSentence(s:Sentence): Unit = {
    super.finishFetchSentence(s)
    s.attr += parse.value.fetchParseTree(s)
  }
}
