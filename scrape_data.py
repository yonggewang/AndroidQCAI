
import json
import re

def parse_markdown(file_path):
    with open(file_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    categories = []
    current_category = None
    
    # Regex for finding links like [Title](url)
    link_pattern = re.compile(r'\[([^\]]+)\]\(([^)]+)\)')

    for line in lines:
        line = line.strip()
        if not line:
            continue
            
        if line.startswith('###'):
            # New Category
            if current_category:
                categories.append(current_category)
            
            category_name = line.replace('###', '').strip()
            current_category = {
                "name": category_name,
                "items": []
            }
        elif line.startswith('-') and current_category is not None:
            # New Item
            content = line.lstrip('- ').strip()
            
            # Extract links
            links = []
            for match in link_pattern.finditer(content):
                links.append({"title": match.group(1), "url": match.group(2)})
            
            # Simple heuristic to find phone numbers
            # This is basic and might need refinement
            phone_pattern = re.compile(r'(\+?1?[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4})')
            phones = phone_pattern.findall(content)
            
            # Add item
            item = {
                "text": content,
                "links": links,
                "phones": phones
            }
            # Avoid duplicates if exactly same content
            if item not in current_category["items"]:
                current_category["items"].append(item)
    
    if current_category:
        categories.append(current_category)

    return categories

if __name__ == "__main__":
    data = parse_markdown("source_content.md")
    with open("data.json", "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print("Successfully generated data.json")
