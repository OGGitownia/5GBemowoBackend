from langchain.prompts import ChatPromptTemplate
from langchain_ollama import OllamaLLM

PROMPT_TEMPLATE = """
Answer the question based only on the following context:


---

Answer the question: {question}
"""


def answer_with_llm(query_text):
    prompt_template = ChatPromptTemplate.from_template(PROMPT_TEMPLATE)
    prompt = prompt_template.format(question=query_text)

    model = OllamaLLM(model="llama3")
    response_text = model.invoke(prompt)

    print("\n **Odpowiedź modelu:**\n")
    print(response_text)
    return response_text


if __name__ == "__main__":
    query = "What is the purpose of RRCConnectionRequest?"
    answer_with_llm(query)
